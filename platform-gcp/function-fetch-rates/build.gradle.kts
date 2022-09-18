plugins {
    id("project-shadow")
}

dependencies {
    runtimeOnly(project(":platform-gcp:store"))
    runtimeOnly(project(":platform-gcp:function"))
    runtimeOnly(project(":fetch-rates"))
}