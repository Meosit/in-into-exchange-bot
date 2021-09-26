import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories { mavenCentral() }

    val kotlinVersion = "1.5.31"
    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}

application {
    mainClassName = "by.mksn.inintobot.MainKt"
}

plugins {
    val kotlinVersion = "1.5.31"
    application
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "4.0.4"
}
repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}


group = "by.mksn.inintobot"
version = "0.0.1"

val betterParseVersion = "0.4.2"
val serializationVersion = "1.3.0"
val ktorVersion = "1.6.3"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.h0tk3y.betterParse:better-parse-jvm:$betterParseVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.6")
    implementation("org.postgresql:postgresql:42.2.1")
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("com.vladsch.kotlin-jdbc:kotlin-jdbc:0.5.2")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

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