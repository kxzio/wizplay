package org.example.audioindex

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.nio.file.*
import kotlin.io.path.*

data class ScannedAudio(
    val path: Path,
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val pos : String,
    val artworkPath: Path? = null
) {
    val albumKey: String
    get() = "$album::$year"
}


private fun norm(path: Path): Path =
    path.toAbsolutePath().normalize()

class AudioFolderController {

    /* ================= CONFIG ================= */

    private val audioExt = setOf("mp3", "wav", "flac", "ogg", "aac", "m4a")
    private val artworkDir = Path("artwork")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /* ================= STATE ================= */

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val roots = mutableSetOf<Path>()

    private val _audioMap =
        MutableStateFlow<Map<Path, ScannedAudio>>(emptyMap())
    val audioMap: StateFlow<Map<Path, ScannedAudio>> = _audioMap

    /** albumKey -> artworkPath */
    private val albumArtworkCache = mutableMapOf<String, Path>()

    private var dirty = false

    /* ================= DTO ================= */

    @Serializable
    private data class AudioDto(
        val path: String,
        val title: String,
        val artist: String,
        val album: String,
        val year: String,
        val pos : String,
        val artworkPath: String?
    )

    @Serializable
    private data class IndexDto(
        val roots: List<String>,
        val audios: List<AudioDto>
    )

    /* ================= STARTUP ================= */

    suspend fun start(config: Path) {
        Files.createDirectories(artworkDir)
        loadFromFile(config)
        saveIfDirty(config)
    }

    fun stop() {
        scope.cancel()
    }

    fun getRoots(): Set<Path> = roots.toSet()

    /* ================= ROOTS ================= */

    suspend fun addRoot(path: Path) {
        val root = norm(path)
        if (!root.isDirectory() || roots.contains(root)) return

        roots.add(root)
        scanRoot(root)
        dirty = true
        saveIfDirty()
    }

    fun removeRoot(path: Path) {
        val root = norm(path)
        if (!roots.remove(root)) return

        val current = _audioMap.value

        // 1. Треки, которые удаляются
        val removedAudios =
            current
                .filterKeys { it.startsWith(root) }
                .values

        // 2. Их albumKey
        val removedAlbumKeys =
            removedAudios
                .map { it.albumKey }
                .toSet()

        // 3. Оставшиеся треки
        val remaining =
            current
                .filterKeys { !it.startsWith(root) }

        // 4. Проверяем, какие albumKey больше нигде не используются
        val stillUsedAlbumKeys =
            remaining.values
                .map { it.albumKey }
                .toSet()

        val albumsToDelete =
            removedAlbumKeys - stillUsedAlbumKeys

        // 5. Удаляем artwork-файлы
        for (albumKey in albumsToDelete) {
            val artworkPath = albumArtworkCache.remove(albumKey)
            if (artworkPath != null) {
                runCatching {
                    Files.deleteIfExists(artworkPath)
                }
            }
        }

        // 6. Обновляем audioMap
        _audioMap.value = remaining

        dirty = true
        saveIfDirty()
    }

    /* ================= SCANNING ================= */

    private suspend fun scanRoot(root: Path) {
        val current = _audioMap.value.toMutableMap()

        Files.walk(root).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { isAudio(it) }
                .forEach { file ->
                    val path = norm(file)
                    if (current.containsKey(path)) return@forEach

                    readTagsAndArtwork(path)?.let {
                        current[path] = it
                    }
                }
        }

        _audioMap.value = current
    }

    suspend fun refreshRoot(
        rootPath: Path,
        onProgress: (current: Int, total: Int) -> Unit
    ) {
        val root = norm(rootPath)
        if (!roots.contains(root)) return

        val existing =
            _audioMap.value
                .filterKeys { it.startsWith(root) }
                .toMutableMap()

        val files = Files.walk(root).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { isAudio(it) }
                .map(::norm)
                .toList()
        }

        val total = files.size
        var currentIndex = 0

        val updated = mutableMapOf<Path, ScannedAudio>()

        for (file in files) {
            currentIndex++
            onProgress(currentIndex, total)

            val old = existing[file]
            if (old != null) {
                updated[file] = old
            } else {
                readTagsAndArtwork(file)?.let {
                    updated[file] = it
                }
            }

            yield()
        }

        _audioMap.value =
            _audioMap.value
                .filterKeys { !it.startsWith(root) } + updated

        dirty = true
        saveIfDirty()
    }

    /* ================= TAGS + ARTWORK ================= */

    private fun readTagsAndArtwork(path: Path): ScannedAudio? =
        try {
            val audio = AudioFileIO.read(path.toFile())
            val tag = audio.tag ?: return null

            val artist = tag.getFirst(FieldKey.ARTIST)
            val album  = tag.getFirst(FieldKey.ALBUM)
            val year   = tag.getFirst(FieldKey.YEAR)
            val title  = tag.getFirst(FieldKey.TITLE)
            val pos    = tag.getFirst(FieldKey.TRACK)

            val albumKey = "$artist::$album::$year"

            // 🔴 ВАЖНО: проверяем ДО извлечения
            val alreadyHadArtwork = albumArtworkCache.containsKey(albumKey)

            val artworkPath =
                albumArtworkCache[albumKey]
                    ?: extractAndCacheArtwork(albumKey, audio)

            ScannedAudio(
                path = path,
                title = title,
                artist = artist,
                album = album,
                year = year,
                pos = pos,
                artworkPath = if (alreadyHadArtwork) null else artworkPath
            )

        } catch (_: Exception) {
            null
        }

    private fun extractAndCacheArtwork(
        albumKey: String,
        audio: org.jaudiotagger.audio.AudioFile
    ): Path? {

        val artwork = audio.tag?.firstArtwork ?: return null
        val data = artwork.binaryData ?: return null

        val fileName =
            albumKey.hashCode().toString().replace("-", "") + ".jpg"
        val target = artworkDir.resolve(fileName)

        if (!Files.exists(target)) {
            Files.write(target, data)
            dirty = true
        }

        albumArtworkCache[albumKey] = target
        return target
    }

    /* ================= JSON ================= */

    fun tracksByAlbum(
        albumKey: String,
    ): List<ScannedAudio> =
        _audioMap.value.values
            .filter { it.albumKey == albumKey }

    fun saveIfDirty(path: Path = Path("folders.config")) {
        if (!dirty) return
        Files.writeString(path, toJson())
        dirty = false
    }

    private fun toJson(): String =
        json.encodeToString(
            IndexDto(
                roots = roots.map { it.toString() },
                audios = _audioMap.value.values.map {
                    AudioDto(
                        path = it.path.toString(),
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        year = it.year,
                        pos = it.pos,
                        artworkPath = it.artworkPath?.toString()
                    )
                }
            )
        )


    private fun loadFromFile(path: Path) {
        if (!Files.exists(path)) return
        loadFromJson(Files.readString(path))
    }

    private fun loadFromJson(text: String) {
        val dto = json.decodeFromString<IndexDto>(text)

        roots.clear()
        roots += dto.roots
            .map { norm(Paths.get(it)) }
            .filter { it.isDirectory() }

        val map = mutableMapOf<Path, ScannedAudio>()

        for (a in dto.audios) {
            val p = norm(Paths.get(a.path))
            val audio = ScannedAudio(
                path = p,
                title = a.title,
                artist = a.artist,
                album = a.album,
                year = a.year,
                pos = a.pos,
                artworkPath = a.artworkPath?.let { Path(it) }
            )
            map[p] = audio

            audio.artworkPath?.let {
                albumArtworkCache[audio.albumKey] = it
            }
        }

        _audioMap.value = map
    }

    /* ================= HELPERS ================= */

    private fun isAudio(path: Path): Boolean =
        path.extension.lowercase() in audioExt
}
