plugins {
    kotlin("jvm")
}

group = "dev.aurakai"
version = "0.2.0-SNAPSHOT"

// Constitutional Rule 12 & 13:
// Keep language-core deterministic, platform-independent, and zero-external-dependency.
dependencies {
    implementation(project(":kernel-core"))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
