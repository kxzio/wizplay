import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io")
    // Добавляем репозиторий JetBrains для Skiko, если его нет в Maven Central
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Media & Metadata
    implementation("org.apache.tika:tika-core:2.9.1")
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("net.jthink:jaudiotagger:3.0.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // JNA (BASS)
    implementation("net.java.dev.jna:jna:5.15.0")
    implementation("net.java.dev.jna:jna-platform:5.15.0")

    // Image Loading
    implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc01")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc01")

    // Haze 1.7.0 (требует Skiko 0.9.x)
    implementation("dev.chrisbanes.haze:haze:1.7.0")
    implementation("dev.chrisbanes.haze:haze-materials:1.7.0")

    // Database
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")

    //linux media controller
    val dbusVersion = "5.2.0" // Или 5.2.0, обе стабильны

    implementation("com.github.hypfvieh:dbus-java-core:$dbusVersion")
    // Используем именно этот транспорт, как рекомендует README
    implementation("com.github.hypfvieh:dbus-java-transport-junixsocket:$dbusVersion")

    // Необходим для работы логов транспорта
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

val os = System.getProperty("os.name").lowercase()
val nativeDir = when {
    os.contains("win") -> "bass/microslop"
    os.contains("linux") -> "bass/linux"
    else -> "bass"
}

val commonRenderJvmArgs = listOf(
    "-Dskiko.vsync.enabled=false",
    "-Dskiko.fps.enabled=false",
)

val linuxRenderJvmArgs = listOf(
    "-Dskiko.renderApi=OPENGL",
    "-Dskiko.linux.opengl.api=GL",
    "-Dcompose.layers.type=component"
)

val windowsRenderJvmArgs = emptyList<String>()

compose.desktop {
    application {
        mainClass = "org.example.MainKt"

        buildTypes.release {
            proguard {
                isEnabled = false
            }
        }

        nativeDistributions {

            targetFormats(
                TargetFormat.AppImage, // Linux
                TargetFormat.Msi,      // Windows
            )

            linux {
                appResourcesRootDir.set(
                    project.layout.projectDirectory.dir("bass/linux")
                )
            }
            windows {
                appResourcesRootDir.set(
                    project.layout.projectDirectory.dir("bass/microslop")
                )
            }

            packageName = "grooviq-desktop"
            packageVersion = "0.1.0"

            jvmArgs(
                "-XX:+UseG1GC",
                "-Xms512m",
                "-Xmx2048m",
                "-XX:+TieredCompilation",
                "-Dsun.java2d.uiScale.enabled=false",
                "-Dskiko.vsync.enabled=false",
            )

            linux {
                jvmArgs(
                    *commonRenderJvmArgs.toTypedArray(),
                    *linuxRenderJvmArgs.toTypedArray()
                )
            }

            windows {
                jvmArgs(
                    *commonRenderJvmArgs.toTypedArray()
                )
            }

        }

        jvmArgs += commonRenderJvmArgs
        jvmArgs += when {
            os.contains("linux") -> linuxRenderJvmArgs
            os.contains("win") -> windowsRenderJvmArgs
            else -> emptyList()
        }

        jvmArgs += listOf(
            "-Xms512m",
            "-Xmx2048m",
            "-Dsun.java2d.uiScale.enabled=false",
            "-Dsun.java2d.dpiaware=true",
            "-Dskiko.vsync.enabled=false",
            "-Dskiko.fps.enabled=false",
            "-Dskiko.fps.limit=144",
            "-Dskiko.render.on.request=true",
            "-Dcompose.interop.blending=true",
            "-XX:+TieredCompilation",
            "-Djna.library.path=${projectDir.absolutePath}/$nativeDir"
            )

    }
}

kotlin {
    jvmToolchain(21)
}

// Принудительно синхронизируем версии JAR и нативной библиотеки
configurations.all {
    resolutionStrategy {
        // Используем версию, которую требует Compose 1.7.3 по умолчанию
        val skikoVersion = "0.9.22.2"
        force("org.jetbrains.skiko:skiko-awt:$skikoVersion")
        force("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:$skikoVersion")
    }
}