plugins {
    id("project-base")
    kotlin("plugin.serialization")
}

val serializationVersion = "1.4.0"
val ktorVersion = "2.1.1"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")

    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}