plugins {
    id("project-base")
    kotlin("plugin.serialization")
}

val serializationVersion: String by project
val ktorVersion: String by project

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")

    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}