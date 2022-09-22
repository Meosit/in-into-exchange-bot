rootProject.name = "in-into-exchange-bot"
include(":common", ":fetch-rates", ":exchange-rates", ":compound-server")
include(":platform-gcp:store", ":platform-gcp:function")
include(":platform-gcp:function-exchange-rates", ":platform-gcp:function-fetch-rates", ":platform-gcp:app-engine")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.google.cloud.tools.appengine")) {
                useModule("com.google.cloud.tools:appengine-gradle-plugin:${requested.version}")
            }
        }
    }
}