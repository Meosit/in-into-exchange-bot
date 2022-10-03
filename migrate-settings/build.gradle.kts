plugins {
    id("function-common")
}

dependencies {
    implementation(project(":common"))
    implementation("org.postgresql:postgresql:42.2.1")
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("com.vladsch.kotlin-jdbc:kotlin-jdbc:0.5.2")
}