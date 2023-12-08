package com.github.thoebert.krosbridgecodegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import java.io.File

val primitiveTypes = mapOf(
    "bool" to BOOLEAN,
    "byte" to BYTE,
    "char" to CHAR,
    "string" to STRING,
    "float32" to FLOAT,
    "float64" to DOUBLE,
    "int8" to BYTE,
    "uint8" to SHORT,
    "int16" to SHORT,
    "uint16" to INT,
    "int32" to INT,
    "uint32" to LONG,
    "int64" to LONG,
    "uint64" to LONG,
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
        writeClass(folder, "${it.name}$requestSuffix", it.request, serviceRequestClassName)
        writeClass(folder, "${it.name}$responseSuffix", it.response, serviceResponseClassName)
    }

    fun writeAction(folder: File, it: Action) {
        val goal = mutableListOf<Field>(Field("actionlib_msgs/GoalID", "goal_id"))
        goal.addAll(it.goal)
        writeClass(folder, "${it.name}Goal", goal, messageClassName)
        writeClass(folder, "${it.name}Result", it.result, messageClassName)
        writeClass(folder, "${it.name}Feedback", it.feedback, messageClassName)
    }

    fun prefixPackage(packageName : String) : String {
        if (packageName.isEmpty()) return packagePrefix
        return "$packagePrefix.$packageName"
    }

    fun writeClass(folder : File, name : String, fields : List<Field>, parentName : ClassName? = null) {
        val (packageName, className) = splitClassNameData(name)

        val classBuilder = TypeSpec.classBuilder(className)
        parentName?.let { classBuilder.superclass(it) }
        classBuilder.addAnnotation(AnnotationSpec.builder(serializableAnnotation).build())
        if (fields.isNotEmpty()) classBuilder.addModifiers(KModifier.DATA)

        val constructor = FunSpec.constructorBuilder()
        fields.filter { it.isVariable }.forEach {
            val mappedType = mapType(it, packageName)
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

        writeClassToFile(folder, classBuilder, prefixPackage(packageName), className)
    }

    private fun writeClassToFile(folder: File, classBuilder : TypeSpec.Builder, packageName: String, className: String){
        val file = FileSpec.builder(packageName, className)
        file.addType(classBuilder.build())
        file.build().writeTo(folder)
    }

    fun mapType(field : Field, currentPackage : String) : TypeName {
        if (field.type == "Header") return ClassName(prefixPackage("std_msgs"),"Header")
        if (field.type == "time") return ClassName(prefixPackage("std_msgs.primitive"),"Time")
        if (field.type == "duration") return ClassName(prefixPackage("std_msgs.primitive"),"Duration")
        val baseType = primitiveTypes[field.type] ?: complexType(field, currentPackage)
        return if (field.isArray) LIST.parameterizedBy(baseType) else baseType
    }

    fun mapPrimitiveType(name : String) : ClassName{
        return primitiveTypes[name] ?: throw IllegalArgumentException("Invalid primitive type $name")
    }

    fun complexType(field : Field, currentPackage : String) : ClassName {
        var (packageName, typeName) = splitClassNameData(field.type)
        if (packageName.isEmpty()){
            packageName = currentPackage
        } else if (defaultPackages.contains(packageName)){
            return ClassName("$defaultPackageName.$packageName", typeName)
        }
        return ClassName(prefixPackage(packageName), typeName)
    }

    fun splitClassNameData(name : String) : Pair<String, String>{
        val index = name.lastIndexOfAny("/\\".toCharArray())
        return if (index == -1) {
            "" to name
        } else {
            name.substring(0, index).replace("/\\\\".toRegex(), ".") to name.substring(index+1)
        }

    }

    fun writeServiceClass(folder: File, service: Service) {
        val (packageName, className) = splitClassNameData(service.name)
        val prefixedPackageName = prefixPackage(packageName)

        val requestClassName = ClassName(prefixedPackageName, "${className}$requestSuffix")
        val responseClassName = ClassName(prefixedPackageName, "${className}$responseSuffix")

        val classBuilder = TypeSpec.classBuilder(className)

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
        val reqParamNames = addParams(service.request, requestFn, packageName)
        requestFn.addStatement("return super.call(%T(%L))", requestClassName, reqParamNames)
        classBuilder.addFunction(requestFn.build())

        val sendResponseFn = FunSpec.builder("sendResponse")
        val respParamNames = addParams(service.response, sendResponseFn, packageName)
        sendResponseFn.addParameter("serviceResult", Boolean::class)
        sendResponseFn.addParameter("serviceId", String::class.asClassName().copy(true))
        sendResponseFn.addStatement("return super.sendResponse(%T(%L), serviceResult, serviceId)", responseClassName, respParamNames)
        classBuilder.addFunction(sendResponseFn.build())

        writeClassToFile(folder, classBuilder, prefixedPackageName, className)

    }

    fun writeTopicClass(folder: File, message: Message) {
        val (packageName, className) = splitClassNameData(message.name)
        val prefixedPackageName = prefixPackage(packageName)

        val messageClassName = ClassName(prefixedPackageName, className)
        val topicClassName = ClassName(prefixedPackageName, "${className}$topicSuffix")

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
        val reqParamNames = addParams(message.fields, publishFn, packageName)
        publishFn.addStatement("return super.publish(%T(%L))", messageClassName, reqParamNames)
        classBuilder.addFunction(publishFn.build())

        writeClassToFile(folder, classBuilder, prefixedPackageName, topicClassName.simpleName)

    }

    private fun addParams(fields : List<Field>, fn : FunSpec.Builder, packageName : String) : String {
        return fields.filter { it.isVariable }.map { f ->
            val pName = f.name
            fn.addParameter(pName, mapType(f, packageName))
            pName
        }.joinToString()
    }

}






