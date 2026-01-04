package org.example

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/* ===================== UTIL ===================== */

private fun norm(path: Path): Path =
    path.toAbsolutePath().normalize()

/* ===================== CONTROLLER ===================== */

class AudioFolderController {

    /* ===================== CONFIG ===================== */

    private val audioExt = setOf("mp3", "wav", "flac", "ogg", "aac", "m4a")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /* ===================== STATE ===================== */

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val roots = mutableSetOf<Path>()

    private val _audioFiles = MutableStateFlow<Set<Path>>(emptySet())
    val audioFiles: StateFlow<Set<Path>> = _audioFiles

    private var dirty = false

    /* ===================== DTO ===================== */

    @Serializable
    private data class AudioIndexDto(
        val roots: List<String>,
        val audioFiles: List<String>
    )

    /* ===================== STARTUP ===================== */

    suspend fun start(configPath: Path) {
        loadFromFile(configPath)
        saveIfDirty(configPath)
    }

    fun stop() {
        scope.cancel()
    }

    fun getRoots(): Set<Path> = roots.toSet()

    /* ===================== ROOT MANAGEMENT ===================== */

    suspend fun addRoot(path: Path) {
        val root = norm(path)
        if (!root.isDirectory() || roots.contains(root)) return

        roots.add(root)
        scanRoot(root)
        dirty = true
        saveIfDirty(Path("folders.config"))
    }

    fun removeRoot(path: Path) {
        val root = norm(path)
        if (!roots.remove(root)) return

        _audioFiles.value = _audioFiles.value
            .filterNot { it.startsWith(root) }
            .toSet()

        dirty = true
    }

    /* ===================== RECONCILE ===================== */

    private suspend fun reconcileWithFileSystem() {
        val known = _audioFiles.value.toSet()
        val found = mutableSetOf<Path>()

        for (root in roots) {
            if (!Files.exists(root)) continue

            Files.walk(root).use { stream ->
                stream
                    .filter { it.isRegularFile() }
                    .filter { isAudio(it) }
                    .map(::norm)
                    .forEach(found::add)
            }
        }

        if (known != found) {
            _audioFiles.value = found
            dirty = true
        }
    }

    /* ===================== SCAN SINGLE ROOT ===================== */

    private suspend fun scanRoot(root: Path) {
        val found = mutableSetOf<Path>()

        Files.walk(root).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { isAudio(it) }
                .map(::norm)
                .forEach(found::add)
        }

        _audioFiles.value = _audioFiles.value + found
    }

    /* ===================== JSON ===================== */

    private fun loadFromFile(path: Path) {
        if (!Files.exists(path)) return
        loadFromJson(Files.readString(path))
    }

    fun saveIfDirty(path: Path) {
        if (!dirty) return
        Files.writeString(path, toJson())
        dirty = false
    }

    private fun toJson(): String {
        val dto = AudioIndexDto(
            roots = roots.map { it.toString() },
            audioFiles = _audioFiles.value.map { it.toString() }
        )
        return json.encodeToString(dto)
    }

    private fun loadFromJson(jsonString: String) {
        val dto = json.decodeFromString<AudioIndexDto>(jsonString)

        val loadedRoots = dto.roots
            .mapNotNull { runCatching { norm(Paths.get(it)) }.getOrNull() }
            .filter { Files.isDirectory(it) }
            .toSet()

        val loadedFiles = dto.audioFiles
            .mapNotNull { runCatching { norm(Paths.get(it)) }.getOrNull() }
            .toSet()

        val validFiles = loadedFiles
            .filter { Files.isRegularFile(it) && isAudio(it) }
            .toSet()

        if (loadedRoots != roots || loadedFiles != validFiles) {
            dirty = true
        }

        roots.clear()
        roots += loadedRoots
        _audioFiles.value = validFiles
    }

    /* ===================== HELPERS ===================== */

    private fun isAudio(path: Path): Boolean =
        path.extension.lowercase() in audioExt

    suspend fun refreshRoot(
        rootPath: Path,
        onProgress: (current: Int, total: Int) -> Unit
    ) {
        val root = norm(rootPath)
        if (!roots.contains(root)) return

        // 1. Сначала считаем total (быстро)
        val allFiles = Files.walk(root).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { isAudio(it) }
                .map(::norm)
                .toList()
        }

        val total = allFiles.size
        val found = mutableSetOf<Path>()

        var current = 0
        for (file in allFiles) {
            found.add(file)
            current++
        }

        val knownInRoot = _audioFiles.value.filter { it.startsWith(root) }.toSet()

        if (knownInRoot != found) {
            _audioFiles.value =
                (_audioFiles.value - knownInRoot + found).toSet()
            dirty = true
        }

        saveIfDirty(Path("folders.config"))
    }
}
