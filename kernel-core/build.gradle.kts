// kernel-core build.gradle.kts — Aligned with ReGenesis Beta Build
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "dev.aurakai.kernel"
version = "0.1.0-alpha"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "25"
    targetCompatibility = "25"
}

tasks.test {
    useJUnitPlatform()
}
