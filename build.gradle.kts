// Root build.gradle.kts — Aligned with ReGenesis Beta Build
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
}

subprojects {
    repositories {
        google()
        mavenCentral()
    }
}
