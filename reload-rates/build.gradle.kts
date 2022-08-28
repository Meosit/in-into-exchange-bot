import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}


dependencies {
    implementation(project(":shared"))
}


tasks {

    named<ShadowJar>("shadowJar") {
        archiveFileName.set("InIntoBot.jar")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "MainKt"))
        }
    }

    register("stage") {
        dependsOn("shadowJar")
    }
}