import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.7.21"
    application
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.thoebert"
version = "1.0"

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

application {
    mainClass.set("MainKt")
}

gradlePlugin {
    plugins {
        create("krosbridge-codegen") {
            id = "com.github.thoebert.krosbridge-codegen"
            implementationClass = "com.github.thoebert.krosbridgecodegen.GradlePlugin"
        }
    }
}