plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.jetbrains.compose") version "1.8.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven (url = "https://jitpack.io")
}

dependencies {

    implementation(compose.desktop.currentOs)

    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("org.apache.tika:tika-core:2.9.1")

    implementation("com.drewnoakes:metadata-extractor:2.19.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    implementation("net.java.dev.jna:jna:5.18.0")
    implementation("net.java.dev.jna:jna-platform:5.18.0")


}


compose.desktop {
    application {
        mainClass = "org.example.MainKt"
        jvmArgs += listOf("-Dsun.java2d.uiScale.enabled=false")
    }
}

kotlin {
    jvmToolchain(21)
}
