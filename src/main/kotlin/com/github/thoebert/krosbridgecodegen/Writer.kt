package com.github.thoebert.krosbridgecodegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import java.io.File

val primitiveTypes = mapOf(
    Type("bool") to BOOLEAN,
    Type("byte") to BYTE,
    Type("char") to CHAR,
    Type(className = "string") to STRING,
    Type("float32") to FLOAT,
    Type("float64") to DOUBLE,
    Type("int8") to BYTE,
    Type("uint8") to SHORT,
    Type("int16") to SHORT,
    Type("uint16") to INT,
    Type("int32") to INT,
    Type("uint32") to LONG,
    Type("int64") to LONG,
    Type("uint64") to LONG,
)

val krosbridgePackageName = "com.github.thoebert.krosbridge"
val messageClassName = ClassName(krosbridgePackageName, "Message")
val serviceRequestClassName = ClassName(krosbridgePackageName, "ServiceRequest")
val serviceResponseClassName = ClassName(krosbridgePackageName, "ServiceResponse")
val defaultPackageName = "com.github.thoebert.krosbridge.messages"
val defaultPackages = listOf(
    "actionlib_msgs",
    "nav_msgs",
    "shape_msgs",
    "stereo_msgs",
    "diagnostic_msgs",
    "rosgraph_msgs",
    "std_msgs",
    "trajectory_msgs",
    "geometry_msgs",
    "sensor_msgs",
    "std_srvs",
    "visualization_msgs",
)

val requestSuffix = "Request"
val responseSuffix = "Response"
val topicSuffix = "Topic"

val serializableAnnotation = ClassName("kotlinx.serialization","Serializable")

class Writer(val packagePrefix : String = ""){
    fun writeRosType(folder: File, it: ROSType) {
        when (it) {
            is Message -> writeMessage(folder, it)
            is Service -> writeService(folder, it)
            is Action -> writeAction(folder, it)
        }
    }

    fun writeMessage(folder: File, it: Message) {
        writeTopicClass(folder, it)
        writeClass(folder, it.name, it.fields, messageClassName)
    }

    fun writeService(folder: File, it: Service) {
        writeServiceClass(folder, it)
        writeClass(folder, it.name.copyWithClassSuffix(requestSuffix), it.request, serviceRequestClassName)
        writeClass(folder, it.name.copyWithClassSuffix(responseSuffix), it.response, serviceResponseClassName)
    }

    fun writeAction(folder: File, it: Action) {
        writeClass(folder, it.name.copyWithClassSuffix("Goal"), it.goal)
        writeClass(folder, it.name.copyWithClassSuffix("Result"), it.result)
        writeClass(folder, it.name.copyWithClassSuffix("Feedback"), it.feedback)
    }

    fun prefixPackage(packageName : String?) : String {
        if (packageName == null) return packagePrefix
        return "$packagePrefix.$packageName"
    }

    fun writeClass(folder : File, name : Type, fields : List<Field>, parentName : ClassName? = null) {

        val classBuilder = TypeSpec.classBuilder(name.className)
        parentName?.let { classBuilder.superclass(it) }
        classBuilder.addAnnotation(AnnotationSpec.builder(serializableAnnotation).build())
        if (fields.isNotEmpty()) classBuilder.addModifiers(KModifier.DATA)

        val constructor = FunSpec.constructorBuilder()
        fields.filter { it.isVariable }.forEach {
            val mappedType = mapType(it, name.packageName)
            constructor.addParameter(it.name, mappedType)
            classBuilder.addProperty(PropertySpec.builder(it.name, mappedType)
                    .initializer(it.name).build())

        }
        classBuilder.primaryConstructor(constructor.build())

        val constants = fields.filter { !it.isVariable }

        if (constants.isNotEmpty()){
            val companionObject = TypeSpec.companionObjectBuilder()
            constants.forEach {
                companionObject.addProperty(
                    PropertySpec.builder(it.name, mapPrimitiveType(it.type))
                        .mutable(false).initializer(it.value!!).build()
                )
            }
            classBuilder.addType(companionObject.build())
        }

        writeClassToFile(folder, classBuilder, prefixPackage(name.packageName), name.className)
    }

    private fun writeClassToFile(folder: File, classBuilder : TypeSpec.Builder, packageName: String, className: String){
        val file = FileSpec.builder(packageName, className)
        file.addType(classBuilder.build())
        file.build().writeTo(folder)
    }

    fun mapType(field : Field, currentPackage : String?) : TypeName {
        if (field.type.equals(Type("Header"))) return ClassName(prefixPackage("std_msgs"),"Header")
        if (field.type.equals(Type("time"))) return ClassName(prefixPackage("std_msgs.primitive"),"Time")
        if (field.type.equals(Type("duration"))) return ClassName(prefixPackage("std_msgs.primitive"),"Duration")
        val baseType = primitiveTypes[field.type] ?: complexType(field, currentPackage)
        return if (field.isArray) LIST.parameterizedBy(baseType) else baseType
    }

    fun mapPrimitiveType(name : Type) : ClassName{
        return primitiveTypes[name] ?: throw IllegalArgumentException("Invalid primitive type $name")
    }

    fun complexType(field : Field, currentPackage : String?) : ClassName {
        var packageName = field.type.packageName
        if (packageName == null){
            packageName = currentPackage
        } else if (defaultPackages.contains(packageName)){
            return ClassName("$defaultPackageName.$packageName", field.type.className)
        }
        return ClassName(prefixPackage(packageName), field.type.className)
    }

    fun writeServiceClass(folder: File, service: Service) {
        val prefixedPackageName = prefixPackage(service.name.packageName)

        val requestClassName = ClassName(prefixedPackageName, service.name.copyWithClassSuffix(requestSuffix).className)
        val responseClassName = ClassName(prefixedPackageName, service.name.copyWithClassSuffix(responseSuffix).className)

        val classBuilder = TypeSpec.classBuilder(service.name.className)

        val constructor = FunSpec.constructorBuilder()
        constructor.addParameter("ros", ClassName(krosbridgePackageName, "Ros"))
        constructor.addParameter("name", String::class)
        classBuilder.primaryConstructor(constructor.build())

        classBuilder.superclass(
            ClassName(krosbridgePackageName, "GenericService")
                .plusParameter(requestClassName)
                .plusParameter(responseClassName)
        ).addSuperclassConstructorParameter("%N", "ros")
            .addSuperclassConstructorParameter("%N", "name")
            .addSuperclassConstructorParameter("%S", service.name)
            .addSuperclassConstructorParameter("%T::class", requestClassName)
            .addSuperclassConstructorParameter("%T::class", responseClassName)


        val requestFn = FunSpec.builder("call")
        requestFn.addModifiers(KModifier.SUSPEND)
        requestFn.returns(Pair::class.asClassName()
                .plusParameter(responseClassName.copy(true))
                .plusParameter(Boolean::class))
        val reqParamNames = addParams(service.request, requestFn, service.name.packageName )
        requestFn.addStatement("return super.call(%T(%L))", requestClassName, reqParamNames)
        classBuilder.addFunction(requestFn.build())

        val sendResponseFn = FunSpec.builder("sendResponse")
        val respParamNames = addParams(service.response, sendResponseFn, service.name.packageName)
        sendResponseFn.addParameter("serviceResult", Boolean::class)
        sendResponseFn.addParameter("serviceId", String::class.asClassName().copy(true))
        sendResponseFn.addStatement("return super.sendResponse(%T(%L), serviceResult, serviceId)", responseClassName, respParamNames)
        classBuilder.addFunction(sendResponseFn.build())

        writeClassToFile(folder, classBuilder, prefixedPackageName, service.name.className)

    }

    fun writeTopicClass(folder: File, message: Message) {
        val prefixedPackageName = prefixPackage(message.name.packageName)

        val messageClassName = ClassName(prefixedPackageName, message.name.className)
        val topicClassName = ClassName(prefixedPackageName, message.name.copyWithClassSuffix(topicSuffix).className)

        val classBuilder = TypeSpec.classBuilder(topicClassName)

        val constructor = FunSpec.constructorBuilder()
        constructor.addParameter("ros", ClassName(krosbridgePackageName, "Ros"))
        constructor.addParameter("name", String::class)
        classBuilder.primaryConstructor(constructor.build())

        classBuilder.superclass(
            ClassName(krosbridgePackageName, "GenericTopic")
                .plusParameter(messageClassName)
        ).addSuperclassConstructorParameter("%N", "ros")
            .addSuperclassConstructorParameter("%N", "name")
            .addSuperclassConstructorParameter("%S", message.name)
            .addSuperclassConstructorParameter("%T::class", messageClassName)

        val publishFn = FunSpec.builder("publish")
        val reqParamNames = addParams(message.fields, publishFn, message.name.packageName)
        publishFn.addStatement("return super.publish(%T(%L))", messageClassName, reqParamNames)
        classBuilder.addFunction(publishFn.build())

        writeClassToFile(folder, classBuilder, prefixedPackageName, topicClassName.simpleName)

    }

    private fun addParams(fields : List<Field>, fn : FunSpec.Builder, packageName : String?) : String {
        return fields.filter { it.isVariable }.map { f ->
            val pName = f.name
            fn.addParameter(pName, mapType(f, packageName))
            pName
        }.joinToString()
    }

}






