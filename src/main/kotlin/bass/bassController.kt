package org.example.bass

import com.sun.jna.Pointer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.audioindex.ScannedAudio
import java.util.concurrent.atomic.AtomicBoolean

data class playlistItem(
    val track: ScannedAudio,
    val audioSource: String
)

data class PlayerState(
    val playlist: List<playlistItem> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val volume: Float = 1.0f
) {
    fun isPlayingItem(item: ScannedAudio): Boolean =
        playlist.getOrNull(index)?.track == item
}

class PlayerController {

    /* ───────────── BASS SYNC ───────────── */

    private val endSync = object : BASS_SYNC_PROC {
        override fun callback(
            handle: Int,
            channel: Int,
            data: Int,
            user: Pointer?
        ) {
            scope.launch {
                onTrackEnded()
            }
        }
    }

    private val bass = Bass.INSTANCE
    private val mix = BassMix.INSTANCE

    private var mixer = 0
    private var decode = 0

    private val playlist = mutableListOf<playlistItem>()
    private var index = -1

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isSeeking = AtomicBoolean(false)

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

        startPositionUpdater()
    }

    /* ───────────── QUEUE ───────────── */

    fun playQueue(list: List<playlistItem>, startIndex: Int = 0) {
        stopInternal()

        playlist.clear()
        playlist.addAll(list)

        index = startIndex.coerceIn(list.indices)
        updateState()

        play(index)
    }

    fun add(item: playlistItem) {
        playlist += item
        updateState()
    }

    fun clear() {
        stop()
        playlist.clear()
        index = -1
        updateState()
    }

    /* ───────────── PLAYBACK ───────────── */

    fun play(i: Int = index) {
        if (playlist.isEmpty()) return

        index = i.coerceIn(playlist.indices)
        updateState()

        playTrack(playlist[index])
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
        stopInternal()
        _state.update { it.copy(isPlaying = false, positionSec = 0.0) }
    }

    fun next() {
        if (index + 1 < playlist.size) play(index + 1)
    }

    fun prev() {
        if (index - 1 >= 0) play(index - 1)
    }

    /* ───────────── SEEK / VOLUME ───────────── */

    fun seek(seconds: Double) {
        if (decode == 0) return

        isSeeking.set(true)

        val bytes = bass.BASS_ChannelSeconds2Bytes(decode, seconds)
        bass.BASS_ChannelSetPosition(decode, bytes, Bass.BASS_POS_BYTE)

        _state.update { it.copy(positionSec = seconds) }

        isSeeking.set(false)
    }

    fun setVolume(v: Float) {
        bass.BASS_ChannelSetAttribute(mixer, Bass.BASS_ATTRIB_VOL, v)
        _state.update { it.copy(volume = v) }
    }

    /* ───────────── INTERNAL ───────────── */

    private fun playTrack(item: playlistItem) {

        bass.BASS_ChannelStop(mixer)
        bass.BASS_ChannelSetPosition(mixer, 0, Bass.BASS_POS_BYTE)

        if (decode != 0) {
            mix.BASS_Mixer_ChannelRemove(decode)
            freeDecode()
        }

        decode = bass.BASS_StreamCreateFile(
            false,
            item.track.path.toString(),
            0,
            0,
            Bass.BASS_STREAM_DECODE or Bass.BASS_SAMPLE_FLOAT
        )

        if (decode == 0) {
            error("Decode create failed: ${bass.BASS_ErrorGetCode()}")
        }

        mix.BASS_Mixer_StreamAddChannel(
            mixer,
            decode,
            BassMix.BASS_MIXER_NORAMPIN or BassMix.BASS_MIXER_DOWNMIX
        )

        bass.BASS_ChannelPlay(mixer, true)

        val lenBytes = bass.BASS_ChannelGetLength(decode, Bass.BASS_POS_BYTE)
        val duration = bass.BASS_ChannelBytes2Seconds(decode, lenBytes)

        bass.BASS_ChannelSetSync(
            decode,
            Bass.BASS_SYNC_END,
            0,
            endSync,
            null
        )

        _state.update {
            it.copy(
                isPlaying = true,
                positionSec = 0.0,
                durationSec = duration
            )
        }
    }

    private fun onTrackEnded() {
        if (index + 1 < playlist.size) {
            play(index + 1)
        } else {
            stop()
        }
    }

    private fun startPositionUpdater() {
        scope.launch {
            while (isActive) {
                if (_state.value.isPlaying && decode != 0 && !isSeeking.get()) {
                    val posBytes =
                        bass.BASS_ChannelGetPosition(decode, Bass.BASS_POS_BYTE)
                    val posSec =
                        bass.BASS_ChannelBytes2Seconds(decode, posBytes)

                    _state.update { it.copy(positionSec = posSec) }
                }
                delay(150)
            }
        }
    }

    private fun stopInternal() {
        bass.BASS_ChannelStop(mixer)
        bass.BASS_ChannelSetPosition(mixer, 0, Bass.BASS_POS_BYTE)

        if (decode != 0) {
            mix.BASS_Mixer_ChannelRemove(decode)
            freeDecode()
        }
    }

    private fun freeDecode() {
        bass.BASS_StreamFree(decode)
        decode = 0
    }

    private fun updateState() {
        _state.update {
            it.copy(
                playlist = playlist.toList(),
                index = index
            )
        }
    }

    /* ───────────── DESTROY ───────────── */

    fun release() {
        scope.cancel()
        stopInternal()
        bass.BASS_Free()
    }
}
