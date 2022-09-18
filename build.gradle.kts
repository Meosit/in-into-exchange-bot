import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.mksn"
version = "1.0"

buildscript {
    repositories { mavenCentral() }

    val kotlinVersion = "1.7.10"
    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

subprojects {

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")
}


allprojects {

    val serializationVersion = "1.4.0"
    val ktorVersion = "2.1.0"

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
        implementation("io.ktor:ktor-client-core:$ktorVersion")
        implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        implementation("io.ktor:ktor-client-java:$ktorVersion")

        implementation("com.google.cloud:google-cloud-firestore:3.4.2")
        api("com.google.cloud.functions:functions-framework-api:1.0.4")

        testImplementation(kotlin("test"))
        testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    }
}


tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

