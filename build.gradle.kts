group = "hut.dev"
version = "0.1.0"

plugins {
    kotlin("jvm") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

kotlin { jvmToolchain(21) }
java   { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

/* Remote only for Paper API + snakeyaml */
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

/* DEBUG: prove Gradle actually sees your local jars */
println("Nova Plugin JAR present? " + file("libs/Nova-0.20.5+MC-1.21.8.jar").absolutePath + " -> " + file("libs/Nova-0.20.5+MC-1.21.8.jar").exists())
println("Nova API JAR present? " + file("libs/nova-api-0.20.5.jar").absolutePath + " -> " + file("libs/nova-api-0.20.5.jar").exists())
println("IA JAR present?   " + file("libs/api-itemsadder-4.0.10.jar").absolutePath + " -> " + file("libs/api-itemsadder-4.0.10.jar").exists())

dependencies {
    // APIs provided by the server at runtime
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    // LOCAL jars (DLL-style)
    compileOnly(files("libs/nova-api-0.20.5.jar"))       // <- the renamed Nova plugin jar
    compileOnly(files("libs/api-itemsadder-4.0.10.jar")) // <- IA API jar (or resolve from devs.beer if you want)
    compileOnly(files("libs/Nova-0.20.5+MC-1.21.8.jar")) // <- Nova Plugin Jar

    implementation(kotlin("stdlib"))
    implementation("org.yaml:snakeyaml:2.2")
}

tasks {
    jar { enabled = false }                 // don’t build the skinny jar
    shadowJar { archiveClassifier.set("") } // produce the main, shaded jar
    build { dependsOn(shadowJar) }
}