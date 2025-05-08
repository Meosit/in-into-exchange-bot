plugins {
    id("project-base")
}

dependencies {
    implementation(project(":common"))
    implementation("com.google.cloud:google-cloud-firestore:3.31.2")
}