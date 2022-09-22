plugins {
    id("server-common")
}

dependencies {
    implementation(project(":exchange-rates"))
    implementation(project(":fetch-rates"))
    implementation(project(":common"))
    implementation("org.slf4j:slf4j-jdk14:2.0.1")
}