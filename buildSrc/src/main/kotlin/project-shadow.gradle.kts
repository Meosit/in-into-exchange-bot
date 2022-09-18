import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("project-base")
    id("com.github.johnrengelman.shadow")
}

tasks {

    named<ShadowJar>("shadowJar") {
        archiveFileName.set("function.jar")
        mergeServiceFiles()
    }

    register("stage") {
        dependsOn("shadowJar")
    }
}