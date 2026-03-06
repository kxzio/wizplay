package org.example.bass.bassController.effects

import org.jaudiotagger.audio.AudioFileIO
import kotlin.math.pow

// REPLAY GAIN

data class ReplayGain(
    val gainDb: Float
)

fun dbToLinear(db: Float): Float =
    10f.pow(db / 20f)

fun parseReplayGain(raw: String?): ReplayGain? {
    if (raw.isNullOrBlank()) return null

    val cleaned = raw
        .lowercase()
        .replace("db", "")
        .trim()

    return cleaned.toFloatOrNull()
        ?.let { ReplayGain(it) }
}

fun readReplayGain(path: String): ReplayGain? {
    return runCatching {
        val file = AudioFileIO.read(java.io.File(path))
        val tag = file.tag ?: return null

        parseReplayGain(
            tag.getFirst("REPLAYGAIN_TRACK_GAIN")
        )
    }.getOrNull()
}

fun defaultReplayGain(): ReplayGain =
    ReplayGain(0f)

fun computeReplayGain(
    replayGain: ReplayGain?,
    preampDb: Float = 0f
): Float {
    if (replayGain == null) return 1f

    val linear = dbToLinear(replayGain.gainDb + preampDb)

    // защита от экстремальных значений
    return linear.coerceIn(
        0.05f, // ≈ -26 dB, ниже уже бессмысленно
        1.5f   // ≈ +3.5 dB, чтобы не клиппило
    )
}

