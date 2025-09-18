rootProject.name = "hut-machines"

pluginManagement {
    repositories {
        gradlePluginPortal()        // <-- this must be present
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }
}
