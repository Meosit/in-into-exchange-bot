plugins {
    id("project-base")
}

dependencies {
    api("com.google.cloud.functions:functions-framework-api:1.0.4")
    implementation(project(":common"))
}