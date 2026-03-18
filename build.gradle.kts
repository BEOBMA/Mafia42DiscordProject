plugins {
    kotlin("jvm") version "2.3.0"
}

group = "org.beobma"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://snapshots.kord.dev")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("dev.kord:kord-core:0.18.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}