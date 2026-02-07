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
    val tracks: List<ScannedAudio>
)

class PlaylistController(
    private val db: AudioDatabase
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
                    tracks = db.tracksInPlaylist(id)
                )
            }
            _playlists.value = result
        }
    }

    fun create(name: String) {
        scope.launch {
            db.createPlaylist(name)
            load()
        }
    }

    fun addTrack(playlistId: Long, track: ScannedAudio) {
        scope.launch {
            val pos = db.tracksInPlaylist(playlistId).size
            db.addTrackToPlaylist(playlistId, track.path, pos)
            load()
        }
    }
}
