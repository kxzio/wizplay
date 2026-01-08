package org.example.audioindex

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.folderGetter.tagsAndAudioGetter.AudioDatabase
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
    val pos: String,
    val artworkPath: Path? = null,
    val albumCreator: Boolean = false
) {
    val albumKey: String
        get() = "$album::$year"
}

private fun norm(path: Path): Path =
    path.toAbsolutePath().normalize()

class AudioFolderController {

    // tools for audio extracting
    private val audioExt = setOf("mp3", "wav", "flac", "ogg", "aac", "m4a")

    //image / caching
    private val artworkDir = Path("artwork")
    private val albumArtworkCache = mutableMapOf<String, Path>()

    //sq lite database
    private val db = AudioDatabase(Path("audio-index.db"))

    //thread controlling to not block ui layer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    //roots added by user
    private val roots = mutableSetOf<Path>()

    //containts only album creators audio files, to get albums tracks use getTracksByAlbum
    private val _audioMap =
        MutableStateFlow<Map<String, ScannedAudio>>(emptyMap())
    val audioMap: StateFlow<Map<String, ScannedAudio>> = _audioMap


    suspend fun start() {

        //create art dir if it not
        Files.createDirectories(artworkDir)

        //clear all roots that could be
        roots.clear()
        roots.addAll(db.loadRoots())

        //get creators by DB, not to load all songs
        val creators = db.loadAlbumCreators()
        _audioMap.value = creators.associateBy { it.albumKey }

        //doing the same to load artworks and get artwork for creators
        albumArtworkCache.clear()
        creators.forEach {
            it.artworkPath?.let { p ->
                albumArtworkCache[it.albumKey] = p
            }
        }

    }

    fun stop() {
        scope.cancel()
        db.close()
    }

    fun getRoots(): Set<Path> = roots.toSet()

    suspend fun addRoot(path: Path) {

        val root = norm(path)
        if (!root.isDirectory() || roots.contains(root)) return

        roots.add(root)
        db.insertRoot(root)

        scanRoot(root)
    }

    fun removeRoot(path: Path) {
        val root = norm(path)
        if (!roots.remove(root)) return

        db.deleteRoot(root)
        db.deleteByRoot(root)

        val creators = db.loadAlbumCreators()
        _audioMap.value = creators.associateBy { it.albumKey }

        albumArtworkCache.clear()
        creators.forEach {
            it.artworkPath?.let { p ->
                albumArtworkCache[it.albumKey] = p
            }
        }
    }

    private suspend fun scanRoot(root: Path) {

        val current = _audioMap.value.toMutableMap()

        Files.walk(root).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { isAudio(it) }
                .forEach { file ->

                    //update all audio files, BUT we dont write them in memory. only in bd
                    val path = norm(file)

                    readTagsAndArtwork(path)?.let { audio ->
                        db.upsertAudio(audio)

                        if (audio.albumCreator && !current.containsKey(audio.albumKey)) {
                            current[audio.albumKey] = audio
                        }
                    }
                }
        }

        //update creators
        _audioMap.value = current
    }

    suspend fun refreshRoot(
        rootPath: Path,
        onProgress: (current: Int, total: Int) -> Unit
    ) {
        val root = norm(rootPath)
        if (!roots.contains(root)) return

        val current = _audioMap.value.toMutableMap()

        val files = Files.walk(root).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { isAudio(it) }
                .map(::norm)
                .toList()
        }

        val total = files.size
        var index = 0

        for (file in files) {
            index++
            onProgress(index, total)

            readTagsAndArtwork(file)?.let { audio ->
                db.upsertAudio(audio)

                //update full songs in bd, but dont update all of them in UI

                if (audio.albumCreator) {
                    current[audio.albumKey] = audio
                }
            }

            yield()
        }

        _audioMap.value = current
    }

    /* ================= TAGS + ARTWORK ================= */

    private fun readTagsAndArtwork(path: Path): ScannedAudio? =
        try {
            val audio = AudioFileIO.read(path.toFile())
            val tag = audio.tag ?: return null

            val artist = tag.getFirst(FieldKey.ARTIST)
            val album = tag.getFirst(FieldKey.ALBUM)
            val year = tag.getFirst(FieldKey.YEAR)
            val title = tag.getFirst(FieldKey.TITLE)
            val pos = tag.getFirst(FieldKey.TRACK)

            val albumKey = "$album::$year"

            val alreadyHasCreator = db.hasAlbumCreator(albumKey)

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
                artworkPath = artworkPath,
                albumCreator = !alreadyHasCreator
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
        }

        albumArtworkCache[albumKey] = target
        return target
    }

    /* ================= QUERIES ================= */

    fun tracksByAlbum(albumKey: String): List<ScannedAudio> =
        db.tracksByAlbum(albumKey)

    /* ================= HELPERS ================= */

    private fun isAudio(path: Path): Boolean =
        path.extension.lowercase() in audioExt
}
