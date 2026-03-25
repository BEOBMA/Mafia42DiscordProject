plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
}

group = "org.beobma"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://snapshots.kord.dev")
}

val ktorVersion = "3.3.1" // 프로젝트에서 사용하는 Ktor 버전으로 통일

dependencies {
    testImplementation(kotlin("test"))

    implementation("dev.kord:kord-core:0.18.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.18")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}