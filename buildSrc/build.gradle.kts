plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = "1.7.10"))
    implementation(kotlin("serialization", version = "1.7.10"))
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
}
