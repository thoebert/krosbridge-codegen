package com.github.thoebert.krosbridgecodegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.File
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

class GradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val outputDir = project.layout.buildDirectory.dir("generated/source/ros/").get().asFile
        val extension = project.extensions.create<KROSBridgeCodegenPluginConfig>("krosbridge-codegen", KROSBridgeCodegenPluginConfig::class.java)
        project.task("generateROSSources") {
            it.doLast {
                val inputDir = File(project.projectDir, "src/${mainSourceSet(project)}/ros/")
                val packageName : String = extension.packageName.get()
                writeAll(inputDir, outputDir, packageName)
            }
        }
    }

    private fun mainSourceSet(project: Project): String {
        return when (getKotlinExtension(project)) {
            is KotlinMultiplatformExtension -> "commonMain"
            else -> "main"
        }
    }

    private fun getKotlinExtension(project : Project) : KotlinProjectExtension? {
        return project.extensions.findByName("kotlin") as KotlinProjectExtension
    }
}

interface KROSBridgeCodegenPluginConfig {
    val packageName: Property<String>
}