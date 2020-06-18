import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    repositories {
        jcenter()
        maven { setUrl("https://kotlin.bintray.com/kotlinx") }
    }
}

application {
    mainClassName = "by.mksn.inintobot.MainKt"
}

plugins {
    application
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}
repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/hotkeytlt/maven")
}


group = "by.mksn.inintobot"
version = "0.0.1"

val betterParseVersion = "0.4.0-alpha-3"
val serializationVersion = "0.20.0"
val ktorVersion = "1.3.2"


dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.h0tk3y.betterParse:better-parse-jvm:$betterParseVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-configparser:$serializationVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveFileName.set("InIntoBot.jar")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "by.mksn.inintobot.MainKt"))
        }
    }

    register("stage") {
        dependsOn("shadowJar")
    }
}