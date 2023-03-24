package com.github.thoebert.krosbridgecodegen

data class Field(val type : String, val name : String, val value : String? = null, val arrayLength : Int = -1){

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
    abstract val name: String
}

data class Message(override val name : String, val fields : List<Field>) : ROSType()

data class Service(override val name : String, val request : List<Field>, val response : List<Field>) : ROSType()

data class Action(override val name : String, val goal : List<Field>, val result : List<Field>, val feedback : List<Field>) : ROSType()