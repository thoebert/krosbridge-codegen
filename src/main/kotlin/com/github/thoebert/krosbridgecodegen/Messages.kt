package com.github.thoebert.krosbridgecodegen

data class Type(val className : String, val packageName : String? = null){
    override fun toString(): String {
        if (packageName == null) return className
        return "$packageName/$className"
    }

    fun copyWithClassSuffix(classSuffix: String) : Type{
        return this.copy(className=className+classSuffix, packageName=packageName)
    }
}

fun createTypeFromString(packageAndClassName : String) : Type{
    val index = packageAndClassName.lastIndexOf("/")
    return if (index == -1) {
        Type(packageAndClassName)
    } else {
        Type(packageName=packageAndClassName.substring(0, index), className=packageAndClassName.substring(index+1))
    }
}

data class Field(val type : Type, val name : String, val value : String? = null, val arrayLength : Int = -1){

    val isArray : Boolean
        get() = arrayLength >= 0
    val hasArrayLength : Boolean
        get() = arrayLength >= 1
    val isVariable : Boolean
        get() = value == null

    override fun toString(): String {
        return "$type $name" + if (value != null) "=$value" else ""
    }
}

abstract class ROSType {
    abstract val name: Type
}

data class Message(override val name : Type, val fields : List<Field>) : ROSType()

data class Service(override val name : Type, val request : List<Field>, val response : List<Field>) : ROSType()

data class Action(override val name : Type, val goal : List<Field>, val result : List<Field>, val feedback : List<Field>) : ROSType()