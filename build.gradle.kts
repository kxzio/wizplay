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
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    implementation("dev.chrisbanes.haze:haze:1.7.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.7.1")

    implementation("org.xerial:sqlite-jdbc:3.51.1.0")

}


compose.desktop {
    application {
        mainClass = "org.example.MainKt"

        jvmArgs += listOf(

            "-XX:+UnlockExperimentalVMOptions",

            "-Dskiko.renderApi=direct3d",
            "-Xms512m",
            "-Xmx2048m",
            "-XX:+UseG1GC",

            "-XX:MaxGCPauseMillis=20",

            "-Dsun.java2d.uiScale.enabled=false",
            "-Dsun.java2d.dpiaware=true",

            "-XX:+DisableExplicitGC",
            "-XX:+AlwaysPreTouch",

            "-XX:+UseG1GC" ,
            "-XX:MaxGCPauseMillis=8",
            "-XX:InitiatingHeapOccupancyPercent=30",
            "-XX:G1NewSizePercent=40",
            "-XX:G1MaxNewSizePercent=60",

            "-XX:+UseStringDeduplication",
            "-XX:+OptimizeStringConcat",

            "-Dskiko.vsync.enabled=true",

            "-Dskiko.direct3d.flushOnPresent=false",

            "-Dskiko.debug=false",
            "-Dskiko.trace.enabled=false",

            "-XX:+TieredCompilation",

            "-Djna.library.path=${projectDir}/bass"
        )
    }
}


kotlin {
    jvmToolchain(21)
}
