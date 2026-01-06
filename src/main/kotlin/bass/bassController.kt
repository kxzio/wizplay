package org.example.bass

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.audioindex.ScannedAudio

data class playlistItem(
    var track : ScannedAudio,
    var audioSource : String,
)
data class PlayerState(
    val playlist: List<playlistItem> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val volume: Float = 1.0f
)

fun PlayerState.isPlaying(track: ScannedAudio, audioSource : String): Boolean =
    index in playlist.indices                           &&
    playlist[index].track == track                      &&
    playlist[index].audioSource == audioSource

class PlayerController {

    private val bass = Bass.INSTANCE
    private val mix = BassMix.INSTANCE

    private var mixer: Int = 0
    private var currentDecode: Int = 0

    private val playlist = mutableListOf<playlistItem>()
    private var index = -1

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /* ───────────── INIT ───────────── */

    fun init() {
        if (!bass.BASS_Init(-1, 44100, 0, 0, 0)) {
            error("BASS_Init failed: ${bass.BASS_ErrorGetCode()}")
        }

        mixer = mix.BASS_Mixer_StreamCreate(
            44100,
            2,
            Bass.BASS_SAMPLE_FLOAT
        )

        if (mixer == 0) {
            error("Mixer create failed: ${bass.BASS_ErrorGetCode()}")
        }

        startPositionPolling()
    }

    /* ───────────── QUEUE ───────────── */

    fun add(track: playlistItem) {
        playlist += track
        updateState()
    }



    fun addAll(tracks: List<playlistItem>) {
        playlist += tracks
        updateState()
    }

    fun removeAt(i: Int) {
        if (i !in playlist.indices) return
        playlist.removeAt(i)
        if (i <= index) index--
        updateState()
    }

    fun clear() {
        stop()
        playlist.clear()
        index = -1
        updateState()
    }

    fun playQueue(tracks: List<playlistItem>, startIndex: Int) {
        if (tracks.isEmpty()) return

        stop()

        playlist.clear()
        playlist.addAll(tracks)

        index = startIndex.coerceIn(playlist.indices)

        updateState()
        play(index)
    }



    /* ───────────── PLAYBACK ───────────── */

    fun play(i: Int = index) {
        if (playlist.isEmpty()) return

        val newIndex = if (i in playlist.indices) i else 0
        index = newIndex

        updateState()

        playTrack(playlist[index])
    }

    private fun playTrack(track: playlistItem) {
        stopCurrentDecode()

        currentDecode = bass.BASS_StreamCreateFile(
            false,
            track.track.path.toString(),
            0,
            0,
            Bass.BASS_STREAM_DECODE or Bass.BASS_SAMPLE_FLOAT
        )

        if (currentDecode == 0) {
            error("Decode create failed: ${bass.BASS_ErrorGetCode()}")
        }

        mix.BASS_Mixer_StreamAddChannel(
            mixer,
            currentDecode,
            BassMix.BASS_MIXER_NORAMPIN
        )

        bass.BASS_ChannelPlay(mixer, false)

        val lenBytes = bass.BASS_ChannelGetLength(currentDecode, Bass.BASS_POS_BYTE)
        val duration = bass.BASS_ChannelBytes2Seconds(currentDecode, lenBytes)

        _state.update {
            it.copy(
                isPlaying = true,
                durationSec = duration
            )
        }
    }

    fun pause() {
        bass.BASS_ChannelPause(mixer)
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        bass.BASS_ChannelPlay(mixer, false)
        _state.update { it.copy(isPlaying = true) }
    }

    fun stop() {
        bass.BASS_ChannelStop(mixer)
        stopCurrentDecode()
        _state.update { it.copy(isPlaying = false, positionSec = 0.0) }
    }

    fun next() {
        if (index + 1 < playlist.size) {
            play(index + 1)
        }
    }

    fun prev() {
        if (index - 1 >= 0) {
            play(index - 1)
        }
    }

    /* ───────────── SEEK / VOLUME ───────────── */

    fun seek(seconds: Double) {
        if (currentDecode == 0) return

        val bytes =
            bass.BASS_ChannelSeconds2Bytes(currentDecode, seconds)

        bass.BASS_ChannelSetPosition(
            currentDecode,
            bytes,
            Bass.BASS_POS_BYTE
        )

        _state.update {
            it.copy(positionSec = seconds)
        }
    }

    fun setVolume(v: Float) {
        bass.BASS_ChannelSetAttribute(mixer, Bass.BASS_ATTRIB_VOL, v)
        _state.update { it.copy(volume = v) }
    }

    /* ───────────── INTERNAL ───────────── */

    private fun stopCurrentDecode() {
        if (currentDecode != 0) {
            mix.BASS_Mixer_ChannelRemove(currentDecode)
            bass.BASS_StreamFree(currentDecode)
            currentDecode = 0
        }
    }

    private fun updateState() {
        _state.update {
            it.copy(
                playlist = playlist,
                index = index
            )
        }
    }

    private fun startPositionPolling() {
        scope.launch {
            while (isActive) {
                if (_state.value.isPlaying && currentDecode != 0) {

                    val posBytes =
                        bass.BASS_ChannelGetPosition(currentDecode, Bass.BASS_POS_BYTE)
                    val lenBytes =
                        bass.BASS_ChannelGetLength(currentDecode, Bass.BASS_POS_BYTE)

                    val posSec =
                        bass.BASS_ChannelBytes2Seconds(currentDecode, posBytes)

                    _state.update { it.copy(positionSec = posSec) }

                    if (posBytes >= lenBytes - 4096) {
                        onTrackEnded()
                    }
                }

                delay(50)
            }
        }
    }

    private fun onTrackEnded() {
        if (index + 1 < playlist.size) {
            play(index + 1)
        } else {
            stop()
        }
    }

    /* ───────────── DESTROY ───────────── */

    fun release() {
        scope.cancel()
        stop()
        bass.BASS_Free()
    }
}
