import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    repositories {
        jcenter()
        maven { setUrl("https://kotlin.bintray.com/kotlinx") }
    }
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:5.0.0")
        val kotlinVersion = "1.3.72"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}
plugins {
    kotlin("multiplatform") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
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
                implementation("io.ktor:ktor-client-json:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")
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
                implementation("com.github.h0tk3y.betterParse:better-parse-jvm:$betterParseVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
                implementation("io.ktor:ktor-client-apache:$ktorVersion")
                implementation("io.ktor:ktor-client-json-jvm:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")

                implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
                implementation("com.amazonaws:aws-lambda-java-events:3.1.0")
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
//                implementation("io.ktor:ktor-client-json-js:$ktorVersion")
//                implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")
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

tasks {
    val shadowCreate by creating(ShadowJar::class) {
        archiveFileName.set("InIntoBotJvm.jar")
        from(kotlin.jvm().compilations.getByName("main").output)
        configurations =
            mutableListOf(kotlin.jvm().compilations.getByName("main").compileDependencyFiles as Configuration)
    }
    val build by existing {
        dependsOn(shadowCreate)
    }
}