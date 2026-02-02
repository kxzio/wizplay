package org.example.bass.bassController

import com.sun.jna.Pointer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.example.OS
import org.example.bass.*
import java.util.concurrent.atomic.AtomicBoolean

/* ───────────── DATA ───────────── */

data class AudioDevice(
    val id: Int,
    val name: String,
    val isDefault: Boolean,
    val driver: String,
    val isEnabled: Boolean
)

data class SavedAudioDevice(
    val name: String,
    val driver: String
)

fun serializeAudioOutput(device: AudioDevice): String {
    val driver = device.driver ?: "default"
    return "$driver|${device.name}"
}

fun deserializeAudioOutput(raw: String): SavedAudioDevice? {
    if (raw.isBlank()) return null

    val parts = raw.split("|", limit = 2)
    if (parts.size != 2) return null

    return SavedAudioDevice(
        driver = parts[0],
        name = parts[1]
    )
}


data class playlistItem(
    val trackPath: String,
    val audioSource: String
)

data class PlayerState(
    val current: playlistItem? = null,
    val isPlaying: Boolean = false,
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val volume: Float = 1f,
    val currentDevice: Int? = -1,
    val audioInfo: PlayingAudioInfo? = null
)

/* ───────────── PLAYER ENGINE ───────────── */

class PlayerController {

    private val bass = Bass.INSTANCE
    private val mix = BassMix.INSTANCE

    /** Один playing stream */
    var mixer = 0

    /** Текущий decode */
    var decodeCurrent = 0

    /** Флаг seek */
    private val isSeeking = AtomicBoolean(false)

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /**
     * Коллбек, который предоставляет СЛЕДУЮЩИЙ трек.
     * QueueController — единственный, кто его реализует.
     */
    var requestNextItem: (() -> playlistItem?)? = null

    var onPauseOrResumeAction: (() -> Unit)? = null

    private var currentDeviceId: Int = -1 // default


    /* ───────────── INIT ───────────── */

    private fun loadPlugin(name: String) {

        val file = if (OS.isLinux) "bass/linux/lib$name${OS.libExt()}" else "bass/microslop/$name${OS.libExt()}"

        val handle = bass.BASS_PluginLoad(file, 0)

        if (handle == 0) {
            println("FAILED to load $file error=${bass.BASS_ErrorGetCode()}")
        } else {
            println("Loaded $file")
        }
    }

    fun resolveDevice(saved: SavedAudioDevice): Int {
        val devices = getAudioDevices()

        // 1. exact match
        devices.firstOrNull {
            it.name == saved.name && it.driver == saved.driver
        }?.let { return it.id }

        // 2. name only
        devices.firstOrNull {
            it.name == saved.name
        }?.let { return it.id }

        // 3. fallback
        return -1 // system default
    }


    fun getAudioDevices(): List<AudioDevice> {
        val devices = mutableListOf<AudioDevice>()
        var i = 0

        val activeDevice = bass.BASS_GetDevice()

        while (true) {
            val info = BASS_DEVICEINFO()
            if (!bass.BASS_GetDeviceInfo(i, info)) break

            val enabled = info.flags and Bass.BASS_DEVICE_ENABLED != 0
            val isDefault = info.flags and Bass.BASS_DEVICE_DEFAULT != 0

            if (enabled) {
                devices += AudioDevice(
                    id = i,
                    driver = info.driver ?: "default",
                    name = info.name ?: "Unknown",
                    isDefault = isDefault,
                    isEnabled = enabled
                )
            }
            i++
        }

        _state.update {
            it.copy(currentDevice = activeDevice)
        }

        return devices
    }

    fun switchAudioDevice(newDeviceId: Int) {
        if (newDeviceId == currentDeviceId) return

        val snapshot = state.value
        val wasPlaying = snapshot.isPlaying
        val item = snapshot.current
        val position = snapshot.positionSec

        scope.launch {
            // 1. Останавливаем всё
            bass.BASS_ChannelStop(mixer)
            bass.BASS_Free()

            // 2. Инициализируем новое устройство
            if (!bass.BASS_Init(newDeviceId, 44100, 0, 0, 0)) {
                error("BASS_Init failed: ${bass.BASS_ErrorGetCode()}")
            }

            currentDeviceId = bass.BASS_GetDevice()

            // 3. Новый mixer
            mixer = mix.BASS_Mixer_StreamCreate(
                44100,
                2,
                Bass.BASS_SAMPLE_FLOAT
            )

            // 4. Если НИЧЕГО не играло — на этом всё
            if (item == null) {
                _state.update {
                    it.copy(currentDevice = currentDeviceId)
                }
                return@launch
            }

            // 5. Иначе — восстанавливаем playback
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

            decodeCurrent = decode
            updateDuration(decode)

            val bytes = bass.BASS_ChannelSeconds2Bytes(decode, position)
            mix.BASS_Mixer_ChannelSetPosition(decode, bytes, Bass.BASS_POS_BYTE)

            if (wasPlaying) {
                bass.BASS_ChannelPlay(mixer, false)
            }

            _state.update {
                it.copy(
                    isPlaying = wasPlaying,
                    positionSec = position,
                    currentDevice = currentDeviceId,
                    audioInfo = getPlayingAudioInfo(decode)
                )
            }
        }
    }



    fun init(saved: SavedAudioDevice?) {

        var deviceId = if (saved != null) resolveDevice(saved) else -1

        currentDeviceId = deviceId

        _state.update {
            it.copy(currentDevice = currentDeviceId)
        }

        if (!bass.BASS_Init(deviceId, 44100, 0, 0, 0)) {
            error("BASS_Init failed: ${bass.BASS_ErrorGetCode()}")
        }

        mixer = mix.BASS_Mixer_StreamCreate(
            44100,
            2,
            Bass.BASS_SAMPLE_FLOAT
        )

        loadPlugin("bassflac")
        loadPlugin("bassopus")
        loadPlugin("basswv")

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

        val newAudioInfo = getPlayingAudioInfo(decodeCurrent)

        _state.update {
            it.copy(
                current = item,
                isPlaying = true,
                positionSec = 0.0,
                audioInfo = newAudioInfo
            )
        }

    }

    fun pause() {
        bass.BASS_ChannelPause(mixer)
        _state.update { it.copy(isPlaying = false) }
        onPauseOrResumeAction?.invoke()
    }

    fun resume() {
        bass.BASS_ChannelPlay(mixer, false)
        _state.update { it.copy(isPlaying = true) }
        onPauseOrResumeAction?.invoke()
    }

    fun stop() {
        stopInternal()
        _state.update {
            PlayerState(currentDevice = currentDeviceId)
        }
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

                val newAudioInfo = getPlayingAudioInfo(decodeCurrent)

                _state.update {
                    it.copy(
                        audioInfo = newAudioInfo,
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
