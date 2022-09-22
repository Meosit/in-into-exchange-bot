import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.application

plugins {
    id("project-base")
    id("com.github.johnrengelman.shadow")
}

tasks {

    jar {
        manifest {
            attributes["Main-Class"] = "io.ktor.server.jetty.EngineMain"
        }
    }

    named<ShadowJar>("shadowJar") {
        archiveFileName.set("server.jar")

        mergeServiceFiles()
    }

    register("stage") {
        dependsOn("shadowJar")
    }
}