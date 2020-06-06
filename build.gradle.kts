buildscript {
    repositories {
        jcenter()
        maven { setUrl("https://kotlin.bintray.com/kotlinx") }
    }
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:4.0.2")
        val kotlinVersion = "1.3.70"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}
plugins {
    kotlin("multiplatform") version "1.3.70"
    kotlin("plugin.serialization") version "1.3.70"
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

apply(plugin = "kotlin-multiplatform")
apply(plugin = "kotlinx-serialization")
apply(plugin = "com.github.johnrengelman.shadow")

kotlin {
    val betterParseVersion = "0.4.0-alpha-3"
    val serializationVersion = "0.20.0"
    val ktorVersion = "1.3.2"
    jvm()
//    js {
//        useCommonJs()
//        nodejs()
//    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("com.ionspin.kotlin:bignum:0.1.5")
                implementation("com.github.h0tk3y.betterParse:better-parse-metadata:$betterParseVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.ktor:ktor-client-mock:$ktorVersion")
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("io.github.config4k:config4k:0.4.1")
                implementation("com.github.h0tk3y.betterParse:better-parse-jvm:$betterParseVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
                implementation("io.ktor:ktor-client-apache:$ktorVersion")
            }
        }
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
            }
        }
//        js().compilations["main"].defaultSourceSet {
//            dependencies {
//                implementation(kotlin("stdlib-js"))
//                implementation("com.github.h0tk3y.betterParse:better-parse-js:$betterParseVersion")
//                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
//                implementation("io.ktor:ktor-client-js:$ktorVersion")
//                implementation(npm("hocon-parser"))
//            }
//        }
//        js().compilations["test"].defaultSourceSet {
//            dependencies {
//                implementation(kotlin("test-js"))
//                implementation("io.ktor:ktor-client-mock-js:$ktorVersion")
//            }
//        }
    }
}