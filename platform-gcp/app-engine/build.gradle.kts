import com.google.cloud.tools.gradle.appengine.appyaml.AppEngineAppYamlExtension

plugins {
    id("server-shadow")
    id("com.google.cloud.tools.appengine") version "2.4.5"
}

dependencies {
    runtimeOnly(project(":platform-gcp:store"))
    runtimeOnly(project(":platform-gcp:function"))
    runtimeOnly(project(":fetch-rates"))
    runtimeOnly(project(":exchange-rates"))
    runtimeOnly(project(":compound-server"))
    implementation("com.google.cloud:google-cloud-logging:3.11.1")
}


tasks.getByPath("appengineStage").dependsOn("shadowJar")

configure<AppEngineAppYamlExtension> {
    stage {
        setArtifact("build/libs/server.jar")
    }
    deploy {
        version = "GCLOUD_CONFIG"
        projectId = "GCLOUD_CONFIG"
    }
}