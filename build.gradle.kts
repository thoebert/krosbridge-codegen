import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.7.21"
    application
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.1.0"
}

group = "io.github.thoebert"
version = "1.0.5"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}


application {
    mainClass.set("MainKt")
}

pluginBundle {
    website = "https://github.com/thoebert/krosbridge-codegen"
    vcsUrl = "https://github.com/thoebert/krosbridge-codegen.git"
    tags = listOf("ros", "code-gen")
}

gradlePlugin {
    plugins {
        create("krosbridge-codegen") {
            id = "io.github.thoebert.krosbridge-codegen"
            displayName = "Code generator for the serialization data classes for krosbridge."
            description = "A code generator for the message/service data classes for krosbridge."
            implementationClass = "com.github.thoebert.krosbridgecodegen.GradlePlugin"
        }
    }
}
