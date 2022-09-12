import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}


dependencies {
    implementation(project(":shared"))
    implementation("com.github.h0tk3y.betterParse:better-parse-jvm:0.4.4")
}


tasks {

    named<ShadowJar>("shadowJar") {
        archiveFileName.set("exchange-rates.jar")
        mergeServiceFiles()
    }

    register("stage") {
        dependsOn("shadowJar")
    }
}