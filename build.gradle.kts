plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.compose") version "1.6.10"
    kotlin("plugin.serialization") version "1.9.24"
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

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // JNA

    // Apache Tika
    implementation("org.apache.tika:tika-core:2.9.1")

    // Metadata extractor
    implementation("com.drewnoakes:metadata-extractor:2.19.0")

    // Coil 3 для Compose Multiplatform

    // 🔑 Коррутины (обязательно для Coil и Compose)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1") // для Compose Desktop
    implementation("org.jetbrains.compose.material:material-icons-extended:1.6.10")
    implementation("org.jetbrains.compose.material3:material3:1.6.10")

    implementation("com.kborowy:kolor-picker:1.0.0")
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
