package org.example.bass.bassController

import com.sun.jna.Pointer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.example.bass.*
import java.util.concurrent.atomic.AtomicBoolean

/* ───────────── DATA ───────────── */

data class playlistItem(
    val trackPath: String,
    val audioSource: String
)

data class PlayerState(
    val current: playlistItem? = null,
    val isPlaying: Boolean = false,
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val volume: Float = 1f
)

/* ───────────── PLAYER ENGINE ───────────── */

class PlayerController {

    private val bass = Bass.INSTANCE
    private val mix = BassMix.INSTANCE

    /** Один playing stream */
    private var mixer = 0

    /** Текущий decode */
    private var decodeCurrent = 0

    /** Флаг seek */
    private val isSeeking = AtomicBoolean(false)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /**
     * Коллбек, который предоставляет СЛЕДУЮЩИЙ трек.
     * QueueController — единственный, кто его реализует.
     */
    var requestNextItem: (() -> playlistItem?)? = null

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

        startPositionUpdater()
    }

    /* ───────────── PLAY CONTROL ───────────── */

    fun play(item: playlistItem) {
        stopInternal()

        val decode = createDecode(item.trackPath)

        bass.BASS_ChannelSetSync(
            decode,
            Bass.BASS_SYNC_END or Bass.BASS_SYNC_MIXTIME,
            0,
            endSync,
            null
        )

        mix.BASS_Mixer_StreamAddChannel(
            mixer,
            decode,
            Bass.BASS_STREAM_AUTOFREE or BassMix.BASS_MIXER_NORAMPIN
        )
        bass.BASS_ChannelSetPosition(mixer, 0, Bass.BASS_POS_BYTE)
        bass.BASS_ChannelPlay(mixer, false)

        decodeCurrent = decode
        updateDuration(decode)

        _state.update {
            it.copy(
                current = item,
                isPlaying = true,
                positionSec = 0.0
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
        stopInternal()
        _state.update { PlayerState() }
    }

    /* ───────────── GAPLESS END SYNC ───────────── */

    private val endSync: BASS_SYNC_PROC = object : BASS_SYNC_PROC {
        override fun callback(handle: Int, channel: Int, data: Int, user: Pointer?) {
            scope.launch {

                val next = requestNextItem?.invoke()
                    ?: run {
                        _state.update { it.copy(isPlaying = false) }
                        return@launch
                    }

                val decode = createDecode(next.trackPath)

                bass.BASS_ChannelSetSync(
                    decode,
                    Bass.BASS_SYNC_END or Bass.BASS_SYNC_MIXTIME,
                    0,
                    endSync,
                    null
                )

                mix.BASS_Mixer_StreamAddChannel(
                    mixer,
                    decode,
                    Bass.BASS_STREAM_AUTOFREE or BassMix.BASS_MIXER_NORAMPIN
                )

                decodeCurrent = decode
                updateDuration(decode)

                _state.update {
                    it.copy(
                        current = next,
                        isPlaying = true,
                        positionSec = 0.0
                    )
                }
            }
        }
    }

    /* ───────────── SEEK ───────────── */

    fun seek(seconds: Double) {
        val handle = decodeCurrent
        if (handle == 0) return

        isSeeking.set(true)

        val bytes = bass.BASS_ChannelSeconds2Bytes(handle, seconds)
        mix.BASS_Mixer_ChannelSetPosition(handle, bytes, Bass.BASS_POS_BYTE)

        _state.update { it.copy(positionSec = seconds) }
        isSeeking.set(false)
    }

    /* ───────────── POSITION UPDATER ───────────── */

    private fun startPositionUpdater() {
        scope.launch {
            while (isActive) {
                if (_state.value.isPlaying && decodeCurrent != 0 && !isSeeking.get()) {
                    val pos = bass.BASS_ChannelGetPosition(
                        decodeCurrent,
                        Bass.BASS_POS_BYTE
                    )
                    val sec = bass.BASS_ChannelBytes2Seconds(decodeCurrent, pos)
                    _state.update { it.copy(positionSec = sec) }
                }
                delay(120)
            }
        }
    }

    /* ───────────── UTILS ───────────── */

    private fun createDecode(path: String): Int =
        bass.BASS_StreamCreateFile(
            false,
            path,
            0,
            0,
            Bass.BASS_STREAM_DECODE or Bass.BASS_SAMPLE_FLOAT
        )

    private fun updateDuration(handle: Int) {
        val len = bass.BASS_ChannelGetLength(handle, Bass.BASS_POS_BYTE)
        val sec = bass.BASS_ChannelBytes2Seconds(handle, len)
        _state.update { it.copy(durationSec = sec) }
    }

    private fun stopInternal() {
        bass.BASS_ChannelStop(mixer)
        if (decodeCurrent != 0) {
            bass.BASS_StreamFree(decodeCurrent)
            decodeCurrent = 0
        }
    }

    fun release() {
        scope.cancel()
        stopInternal()
        bass.BASS_Free()
    }
}
