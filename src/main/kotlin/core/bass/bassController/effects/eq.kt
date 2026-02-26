import org.example.bass.BASS_BFX_PEAKEQ
import org.example.bass.BassFX

val EQ_BANDS = floatArrayOf(
    31f, 62f, 125f, 250f, 500f,
    1000f, 2000f, 4000f, 8000f, 16000f
)

val eqFxHandles = IntArray(10)

fun initEqualizer(channel: Int) {
    for (i in EQ_BANDS.indices) {
        val fx = BassFX.INSTANCE.BASS_ChannelSetFX(
            channel,
            BassFX.BASS_FX_BFX_PEAKEQ,
            i
        )

        eqFxHandles[i] = fx

        val params = BASS_BFX_PEAKEQ(
            fCenter = EQ_BANDS[i],
            fBandwidth = 2.5f,
            fGain = 0f,
            lChannel = -1
        )

        params.write()
        BassFX.INSTANCE.BASS_FXSetParameters(fx, params.pointer)
    }
}

fun setEqBand(band: Int, gainDb: Float) {
    val fx = eqFxHandles[band]
    if (fx == 0) return

    val params = BASS_BFX_PEAKEQ(
        fCenter = EQ_BANDS[band],
        fBandwidth = 2.5f,
        fGain = gainDb.coerceIn(-12f, 12f),
        lChannel = -1
    )

    params.write()
    BassFX.INSTANCE.BASS_FXSetParameters(fx, params.pointer)
}
