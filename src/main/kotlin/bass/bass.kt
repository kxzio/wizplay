package org.example.bass

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Callback
import com.sun.jna.Pointer
import com.sun.jna.Structure

interface BASS_SYNC_PROC : Callback {
    fun callback(handle: Int, channel: Int, data: Int, user: Pointer?)
}

fun floatWaveToBytes(wave: FloatArray): ByteArray {
    val out = ByteArray(wave.size)
    for (i in wave.indices) {
        // BASS обычно даёт -1..1
        val v = (wave[i] * 127f)
            .toInt()
            .coerceIn(-128, 127)
        out[i] = v.toByte()
    }
    return out
}


@Structure.FieldOrder(
    "freq",
    "chans",
    "flags",
    "ctype",
    "origres",
    "plugin",
    "sample"
)

class BASS_CHANNELINFO : Structure() {
    @JvmField var freq = 0        // sample rate
    @JvmField var chans = 0       // channels count
    @JvmField var flags = 0       // flags (FLOAT, etc)
    @JvmField var ctype = 0       // stream type
    @JvmField var origres = 0     // original resolution (bits)
    @JvmField var plugin = 0      // plugin handle
    @JvmField var sample = 0      // sample handle
}

@Structure.FieldOrder("name", "driver", "flags")
class BASS_DEVICEINFO : Structure() {
    @JvmField var name: String? = null
    @JvmField var driver: String? = null
    @JvmField var flags: Int = 0
}

class BassFloatBuffer(size: Int) {
    val memory = com.sun.jna.Memory(size.toLong() * 4)
    val array = FloatArray(size)
}

fun Bass.getData(
    handle: Int,
    buf: BassFloatBuffer,
    length: Int
): Int {
    val read = this.BASS_ChannelGetData(handle, buf.memory, length)
    buf.memory.read(0, buf.array, 0, buf.array.size)
    return read
}



interface Bass : Library {

    fun BASS_GetDeviceInfo(device: Int, info: BASS_DEVICEINFO): Boolean
    fun BASS_SetDevice(device: Int): Boolean
    fun BASS_GetDevice(): Int

    fun BASS_PluginLoad(file: String, flags: Int): Int

    fun BASS_Init(device: Int, freq: Int, flags: Int, win: Long, clsid: Long): Boolean
    fun BASS_Free(): Boolean
    fun BASS_ErrorGetCode(): Int

    fun BASS_ChannelGetInfo(handle: Int, info: BASS_CHANNELINFO): Boolean

    fun BASS_StreamCreateFile(
        mem: Boolean,
        file: String,
        offset: Long,
        length: Long,
        flags: Int
    ): Int

    fun BASS_StreamFree(handle: Int): Boolean

    fun BASS_ChannelPlay(handle: Int, restart: Boolean): Boolean
    fun BASS_ChannelPause(handle: Int): Boolean
    fun BASS_ChannelStop(handle: Int): Boolean

    fun BASS_ChannelGetPosition(handle: Int, mode: Int): Long
    fun BASS_ChannelGetLength(handle: Int, mode: Int): Long
    fun BASS_ChannelSetPosition(handle: Int, pos: Long, mode: Int): Boolean

    fun BASS_ChannelBytes2Seconds(handle: Int, pos: Long): Double
    fun BASS_ChannelSeconds2Bytes(handle: Int, pos: Double): Long

    fun BASS_ChannelSetAttribute(handle: Int, attrib: Int, value: Float): Boolean
    fun BASS_ChannelGetAttribute(handle: Int, attrib: Int, value: FloatArray): Boolean

    fun BASS_ChannelSetSync(
        handle: Int,
        type: Int,
        param: Long,
        proc: BASS_SYNC_PROC,
        user: Pointer? = null
    ): Int

    fun BASS_StreamCreate(
        freq: Int,
        chans: Int,
        flags: Int,
        proc: Pointer,
        user: Pointer?
    ): Int

    fun BASS_ChannelGetData(
        handle: Int,
        buffer: Pointer,
        length: Int
    ): Int

    fun BASS_StreamPutData(
        handle: Int,
        buffer: Pointer,
        length: Int
    ): Int


    companion object {
        val INSTANCE: Bass = Native.load("bass", Bass::class.java)
        const val BASS_DATA_FLOAT = 0x40000000
        const val BASS_SAMPLE_FLOAT = 0x100
        const val STREAMPROC_PUSH = -1

        const val BASS_SYNC_MIXTIME = 0x40000000
        const val BASS_STREAM_AUTOFREE = 0x40000

        const val BASS_STREAM_DECODE = 0x200000
        const val BASS_STREAM_PRESCAN = 0x20000
        const val BASS_POS_BYTE = 0
        const val BASS_MIXER_END = 0x10000

        const val BASS_ATTRIB_VOL = 2

        const val BASS_SYNC_END = 2

        // Channel types
        const val BASS_CTYPE_STREAM = 0x10000
        const val BASS_CTYPE_STREAM_MP3  = 0x10000
        const val BASS_CTYPE_STREAM_OGG  = 0x10002
        const val BASS_CTYPE_STREAM_WAV  = 0x10004
        const val BASS_CTYPE_STREAM_FLAC = 0x10900
        const val BASS_CTYPE_STREAM_AAC  = 0x10B00

        // Attributes
        const val BASS_ATTRIB_BITRATE = 0x400000
        const val BASS_ATTRIB_FREQ    = 1

        const val BASS_UNICODE = 0x80000000.toInt()

        const val BASS_DATA_FFT2048 = 0x80000003.toInt()

        const val BASS_DEVICE_ENABLED = 1
        const val BASS_DEVICE_DEFAULT = 2
        
        const val BASS_DATA_FFT256   = 0x80000000.toInt() or 256
        const val BASS_DATA_FFT512   = 0x80000000.toInt() or 512
        const val BASS_DATA_FFT1024  = 0x80000000.toInt() or 1024
        const val BASS_DATA_FFT4096  = 0x80000000.toInt() or 4096
    }
}

interface BassMix : Library {

    fun BASS_Mixer_StreamCreate(
        freq: Int,
        chans: Int,
        flags: Int
    ): Int

    fun BASS_Mixer_StreamAddChannel(
        mixer: Int,
        channel: Int,
        flags: Int
    ): Boolean

    fun BASS_Mixer_ChannelRemove(channel: Int): Boolean

    fun BASS_Mixer_ChannelSetPosition(
        channel: Int,
        pos: Long,
        mode: Int
    ): Boolean

    companion object {
        val INSTANCE: BassMix =
            Native.load("bassmix", BassMix::class.java)

        const val BASS_MIXER_NORAMPIN = 0x1000
        const val BASS_MIXER_DOWNMIX = 0x400000
        const val BASS_MIXER_QUEUE   = 0x2000
    }
}


