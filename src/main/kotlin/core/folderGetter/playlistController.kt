package org.example.folderGetter

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.audioindex.ScannedAudio
import org.example.folderGetter.tagsAndAudioGetter.AudioDatabase

@Stable
data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int
){
    val playlistKey: String
        get() = "_playlist:$name::$id"
}

class PlaylistController(
    val db: AudioDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _playlists =
        MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    fun load() {
        scope.launch {
            val result = db.loadPlaylists().map { (id, name) ->
                Playlist(
                    id = id,
                    name = name,
                    trackCount = db.playlistTrackCount(id)
                )
            }
            _playlists.value = result
        }
    }

    fun getPlaylistById(
        playlistId : Long
    ) : Playlist? {
        return playlists.value.firstOrNull { it.id == playlistId }
    }

    fun create(name: String) {
        scope.launch {
            db.createPlaylist(name)
            load()
        }
    }

    fun tracksByPlaylist(playlistId: Long): List<ScannedAudio> =
        db.tracksInPlaylist(playlistId)

    fun addTrack(playlistId: Long, track: ScannedAudio) {
        scope.launch {
            val pos = db.tracksInPlaylist(playlistId).size
            db.addTrackToPlaylist(playlistId, track.path, pos)
            load()
        }
    }

    fun delete(id: Long) {
        scope.launch {
            db.deletePlaylist(id)
            _playlists.value = _playlists.value.filter { it.id != id }
        }
    }


    fun rename(id: Long, newName: String) {
        scope.launch {
            db.renamePlaylist(id, newName)

            _playlists.value = _playlists.value.map {
                if (it.id == id)
                    it.copy(name = newName)
                else
                    it
            }
        }
    }
}
