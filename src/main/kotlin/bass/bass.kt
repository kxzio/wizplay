package org.example.bass

import com.sun.jna.Library
import com.sun.jna.Native

interface Bass : Library {

    fun BASS_Init(device: Int, freq: Int, flags: Int, win: Long, clsid: Long): Boolean
    fun BASS_Free(): Boolean
    fun BASS_ErrorGetCode(): Int

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

    companion object {
        val INSTANCE: Bass =
            Native.load("bass", Bass::class.java)

        const val BASS_SAMPLE_FLOAT = 0x100
        const val BASS_STREAM_DECODE = 0x200000
        const val BASS_POS_BYTE = 0
        const val BASS_ATTRIB_VOL = 2
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

    companion object {
        val INSTANCE: BassMix =
            Native.load("bassmix", BassMix::class.java)

        const val BASS_MIXER_NORAMPIN = 0x1000
    }
}

