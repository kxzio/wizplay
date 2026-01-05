package org.example.bass

import com.sun.jna.Library
import com.sun.jna.Native

interface Bass : Library {

    fun BASS_Init(
        device: Int,
        freq: Int,
        flags: Int,
        win: Long,
        clsid: Long
    ): Boolean

    fun BASS_Free(): Boolean

    fun BASS_ErrorGetCode(): Int

    fun BASS_StreamCreateFile(
        mem: Boolean,
        file: String,
        offset: Long,
        length: Long,
        flags: Int
    ): Int

    fun BASS_ChannelPlay(
        handle: Int,
        restart: Boolean
    ): Boolean

    fun BASS_ChannelStop(handle: Int): Boolean

    companion object {
        val INSTANCE: Bass =
            Native.load("bass", Bass::class.java)
    }
}
