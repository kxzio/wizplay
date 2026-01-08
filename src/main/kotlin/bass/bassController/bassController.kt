package org.example.bass.bassController

import com.sun.jna.Pointer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.example.audioindex.ScannedAudio
import org.example.bass.*
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
    val volume: Float = 1f
) {
    fun isPlayingItem(item: ScannedAudio): Boolean =
        playlist.getOrNull(index)?.track == item
}

class PlayerController {

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

    fun init() {
        if (!bass.BASS_Init(-1, 44100, 0, 0, 0))
            error("BASS_Init failed")

        mixer = mix.BASS_Mixer_StreamCreate(
            44100, 2, Bass.BASS_SAMPLE_FLOAT
        )

        startPositionUpdater()
    }

    private val endSync = object : BASS_SYNC_PROC {
        override fun callback(
            handle: Int,
            channel: Int,
            data: Int,
            user: Pointer?
        ) {
            scope.launch {
                nextInternal()
            }
        }
    }

    fun playQueue(list: List<playlistItem>, startIndex: Int) {
        stopInternal()

        playlist.clear()
        playlist.addAll(list)

        index = startIndex.coerceIn(list.indices)
        updateState()
        playInternal()
    }

    private fun playInternal() {
        if (playlist.isEmpty()) return

        if (decode != 0) {
            mix.BASS_Mixer_ChannelRemove(decode)
            bass.BASS_StreamFree(decode)
        }

        decode = bass.BASS_StreamCreateFile(
            false,
            playlist[index].track.path.toString(),
            0,
            0,
            Bass.BASS_STREAM_DECODE or
                    Bass.BASS_SAMPLE_FLOAT or
                    Bass.BASS_STREAM_PRESCAN   // 🔥 ВАЖНО
        )

        mix.BASS_Mixer_StreamAddChannel(
            mixer, decode, BassMix.BASS_MIXER_DOWNMIX
        )

        bass.BASS_ChannelSetSync(
            decode, Bass.BASS_SYNC_END, 0, endSync, null
        )

        bass.BASS_ChannelPlay(mixer, true)
        updateDuration()

        _state.update { it.copy(isPlaying = true, positionSec = 0.0) }
    }

    private fun nextInternal() {
        if (index + 1 !in playlist.indices) return
        index++
        updateState()
        playInternal()
    }

    fun next() {
        nextInternal()
    }

    fun prev() {
        if (index - 1 !in playlist.indices) return
        index--
        updateState()
        playInternal()
    }

    fun pause() {
        bass.BASS_ChannelPause(mixer)
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        bass.BASS_ChannelPlay(mixer, false)
        _state.update { it.copy(isPlaying = true) }
    }

    fun seek(seconds: Double) {
        if (decode == 0) return

        isSeeking.set(true)
        val bytes = bass.BASS_ChannelSeconds2Bytes(decode, seconds)
        bass.BASS_ChannelSetPosition(decode, bytes, Bass.BASS_POS_BYTE)
        _state.update { it.copy(positionSec = seconds) }
        isSeeking.set(false)
    }

    private fun startPositionUpdater() {
        scope.launch {
            while (isActive) {
                if (_state.value.isPlaying && decode != 0 && !isSeeking.get()) {
                    val pos = bass.BASS_ChannelGetPosition(decode, Bass.BASS_POS_BYTE)
                    val sec = bass.BASS_ChannelBytes2Seconds(decode, pos)
                    _state.update { it.copy(positionSec = sec) }
                }
                delay(150)
            }
        }
    }

    private fun updateDuration() {
        val len = bass.BASS_ChannelGetLength(decode, Bass.BASS_POS_BYTE)
        val sec = bass.BASS_ChannelBytes2Seconds(decode, len)
        _state.update { it.copy(durationSec = sec) }
    }

    private fun stopInternal() {
        bass.BASS_ChannelStop(mixer)
        if (decode != 0) {
            bass.BASS_StreamFree(decode)
            decode = 0
        }
    }

    private fun updateState() {
        _state.update {
            it.copy(playlist = playlist.toList(), index = index)
        }
    }

    fun release() {
        scope.cancel()
        stopInternal()
        bass.BASS_Free()
    }
}
