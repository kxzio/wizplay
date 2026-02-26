package org.example.bass.bassController

import org.example.bass.BASS_CHANNELINFO
import org.example.bass.Bass
import org.example.bass.Bass.Companion.BASS_ATTRIB_BITRATE
import org.example.bass.Bass.Companion.BASS_CTYPE_STREAM
import org.example.bass.Bass.Companion.BASS_CTYPE_STREAM_AAC
import org.example.bass.Bass.Companion.BASS_CTYPE_STREAM_FLAC
import org.example.bass.Bass.Companion.BASS_CTYPE_STREAM_MP3
import org.example.bass.Bass.Companion.BASS_CTYPE_STREAM_OGG
import org.example.bass.Bass.Companion.BASS_CTYPE_STREAM_WAV
import java.nio.file.Files
import java.nio.file.Path

data class PlayingAudioInfo(
    val format: String ,
    val sampleRate: Int,
    val channels: Int,
    val bitDepth: Int,
    val bitrateKbps: Int?,
    val isFloat: Boolean,
    val durationSec: Double
)

private fun PlayingAudioInfo.isLossless(): Boolean =
    format == "FLAC" || format == "WAV"

fun PlayingAudioInfo.prettyLossless(): String =
    listOf(
        format,
        String.format("%.1f kHz", sampleRate / 1000f),
        "$bitDepth-bit",
        "$bitrateKbps kbps"
    ).joinToString(" · ")

fun PlayingAudioInfo.prettyLossy(): String =
    listOf(
        format,
        String.format("%.1f kHz", sampleRate / 1000f),
        "decoded"
    ).joinToString(" · ")

fun PlayingAudioInfo.prettyString(): String =
    if (isLossless()) prettyLossless() else prettyLossy()

fun getPlayingAudioInfo(stream: Int): PlayingAudioInfo? {
    val bass = Bass.INSTANCE

    val info = BASS_CHANNELINFO()
    if (!bass.BASS_ChannelGetInfo(stream, info)) return null

    val isFloat = info.flags and Bass.BASS_SAMPLE_FLOAT != 0

    val bitDepth =
        if (info.origres != 0) info.origres
        else if (isFloat) 32 else 16

    val format = when {
        // ─── Plugin formats
        info.plugin != 0 -> when (info.ctype) {
            BASS_CTYPE_STREAM_FLAC -> "FLAC"
            BASS_CTYPE_STREAM_AAC  -> "AAC"
            else -> "PLUGIN"
        }

        // ─── WAV (STREAM или SAMPLE)
        info.ctype == BASS_CTYPE_STREAM_WAV ||
                info.ctype == 0x50003 -> "WAV"

        // ─── OGG (STREAM)
        info.ctype == BASS_CTYPE_STREAM_OGG -> "OGG"

        // ─── MP3 (STREAM диапазон)
        info.ctype in 0x10000..0x10005 -> "MP3"

        else -> "UNKNOWN (0x${info.ctype.toString(16)})"
    }

    val bitrate = when (format) {
        "FLAC", "WAV" -> {
            // PCM bitrate
            (info.freq * bitDepth * info.chans) / 1000
        }

        "MP3", "OGG", "AAC" -> {
            // Average file bitrate
            val len = bass.BASS_ChannelGetLength(stream, Bass.BASS_POS_BYTE)
            val dur = bass.BASS_ChannelBytes2Seconds(stream, len)
            if (dur > 0) ((len * 8) / dur / 1000).toInt() else null
        }

        else -> null
    }

    // ─── Duration ───
    val lenBytes = bass.BASS_ChannelGetLength(stream, Bass.BASS_POS_BYTE)
    val duration = bass.BASS_ChannelBytes2Seconds(stream, lenBytes)

    return PlayingAudioInfo(
        format = format,
        sampleRate = info.freq,
        channels = info.chans,
        bitDepth = bitDepth,
        bitrateKbps = bitrate,
        isFloat = isFloat,
        durationSec = duration
    )
}
