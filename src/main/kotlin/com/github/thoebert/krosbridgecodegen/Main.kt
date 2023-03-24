package com.github.thoebert.krosbridgecodegen

import java.io.File

fun main(args: Array<String>) {
    if (args.size != 3){
        System.err.println("Please provide the following parameters inputfolder outputfolder packageprefix")
    }
    val inputFolder = File(args[0])
    val outputFolder = File(args[1])
    writeAll(inputFolder, outputFolder, args[2])
}

fun writeAll(inputFolder : File, outputFolder : File, packagePrefix : String = ""){
    println("Generating ROS Sources with packageName $packagePrefix from $inputFolder into $outputFolder")
    val writer = Writer(packagePrefix)
    readTypes(inputFolder).forEach {
        writer.writeRosType(outputFolder, it)
    }
}

fun readTypes(folder : File): Sequence<ROSType> {
    return folder.walk().map {
        var rosType : ROSType? = null
        if (it.isFile && !it.isHidden){
            val extension = it.extension
            val filename = it.name.removeSuffix("."+extension)
            val name = it.relativeTo(folder).parentFile.parentFile.resolve(filename).toString()
            val fileContent = it.readText()
            rosType = when (it.extension) {
                "msg" -> parseMessage(name, fileContent)
                "srv" -> parseService(name, fileContent)
                "action" -> parseAction(name, fileContent)
                else -> null
            }
        }
        return@map rosType
    }.filterNotNull()
}