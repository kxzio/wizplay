package org.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Fullscreen
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Repeat
import androidx.compose.material.icons.sharp.RepeatOne
import androidx.compose.material.icons.sharp.Shuffle
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material.icons.sharp.SurroundSound
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Dp.Companion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import core.coreMaster.grooviqCore
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import org.example.bass.Bass
import org.example.bass.Bass.Companion.BASS_DATA_FFT2048
import org.example.bass.BassFloatBuffer
import org.example.bass.bassController.prettyString
import org.example.bass.floatWaveToBytes
import org.example.bass.getData
import org.example.bass.queue.repeatMods
import org.example.dominantColorFromPathStable
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.ui.uiHelpers.AnimSpeed
import org.example.ui.uiHelpers.BarVisualizer
import org.example.ui.uiHelpers.BlobVisualizer
import org.example.ui.uiHelpers.PaintStyle
import org.example.ui.uiHelpers.VisualizerStyle
import org.example.wizui.wizui.FlatSliderTrack
import kotlin.io.path.pathString
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

val fftBuf = BassFloatBuffer(1024)
val waveBuf = BassFloatBuffer(1024)

private const val BLOB_POINTS = 48

private val smoothEnergy = FloatArray(BLOB_POINTS)
private var fftRotation = 0f

val longSmooth = FloatArray(BLOB_POINTS) { 0f }

fun waveformToEnergy(
    wave: FloatArray,
    points: Int
): FloatArray {
    val out = FloatArray(points)
    val step = wave.size / points
    for (i in 0 until points) {
        var sum = 0f
        var count = 0
        val start = i * step
        val end = (start + step).coerceAtMost(wave.size)
        for (j in start until end) {
            val v = wave[j]
            sum += v * v  // RMS
            count++
        }
        out[i] = if (count > 0) sqrt(sum / count) else 0f
    }
    return out
}

fun fftToBlobBytes(
    fft: FloatArray,
    wave: FloatArray,
    compression: Float  = 0.0f
): ByteArray {

    val FFT_START = 2
    val FFT_END = 1024

    val BLOB_POINTS = BLOB_POINTS  // твоя константа
    val out = ByteArray(BLOB_POINTS)

    val raw = FloatArray(BLOB_POINTS)
    val smooth = FloatArray(BLOB_POINTS)
    val blur = FloatArray(BLOB_POINTS)

    val logMin = ln(FFT_START.toFloat())
    val logMax = ln(FFT_END.toFloat())

    // ---------- FFT → ENERGY ----------
    for (i in 0 until BLOB_POINTS) {
        val u0 = i.toFloat() / BLOB_POINTS
        val u1 = (i + 1).toFloat() / BLOB_POINTS

        val startExp = exp(logMin + u0 * (logMax - logMin))
        val endExp = exp(logMin + u1 * (logMax - logMin))

        val startBin = startExp.toInt()
        val endBin = endExp.toInt()

        var power = 0f
        for (b in startBin.coerceAtLeast(1) until endBin.coerceAtMost(fft.size)) {
            val mag = fft[b]
            power += mag * mag
        }

        raw[i] = sqrt(power)
    }

    // ---------- TEMPORAL SMOOTH ----------
    val attack = 0.4f
    val release = 0.08f

    for (i in 0 until BLOB_POINTS) {
        val prev = smoothEnergy[i]
        val target = raw[i]

        smoothEnergy[i] = if (target > prev) {
            prev + (target - prev) * attack
        } else {
            prev + (target - prev) * release
        }

        smooth[i] = smoothEnergy[i]
    }

    // ---------- SPATIAL BLUR ----------
    for (i in 0 until BLOB_POINTS) {
        val p = smooth[(i - 1 + BLOB_POINTS) % BLOB_POINTS]
        val c = smooth[i]
        val n = smooth[(i + 1) % BLOB_POINTS]
        blur[i] = c * 0.75f + p * 0.125f + n * 0.125f
    }

    // ---------- SCALE + COMPRESSION ----------
    val baseGain = 2.0f
    val comp = compression.coerceIn(0f, 1f)

    // чем больше compression — тем сильнее лог-кривая
    val k = lerp(1.5f, 6f, comp)

    for (i in 0 until BLOB_POINTS) {
        var e = blur[i] * baseGain

        // bass emphasis (физически корректно)
        if (i < BLOB_POINTS * 0.25f) {
            e *= lerp(1.8f, 1.0f, comp)
        }

        // unified compression
        val compressed =
            ln(1f + e * k) / ln(1f + k)

        val v = (compressed * 255f)
            .coerceIn(0f, 255f)
            .toInt()

        out[i] = v.toByte()
    }

    return out
}

fun Modifier.verticalFadeBottomUp(
    alpha: Float = 1f,
    startFraction: Float = 0.0f, // 0 = низ
    endFraction: Float = 0.3f    // насколько вверх идёт fade
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha), // 🔥 важно: WHITE
                    Color.Transparent
                ),
                startY = size.height * (1f - endFraction),
                endY = size.height * (1f - startFraction)
            ),
            blendMode = BlendMode.DstIn
        )
    }
)

fun Modifier.radialFadeMask(
    radiusFraction: Float = 0.5f, // радиус круга относительно min(size)
    softness: Float = 0.4f        // насколько мягкие края
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()

        val minSize = minOf(size.width, size.height)
        val radius = minSize * radiusFraction
        val fadeRadius = radius * (1f + softness)

        drawRect(
            brush = Brush.radialGradient(
                0f to Color.White,
                (radius / fadeRadius) to Color.White,
                1f to Color.Transparent,
                center = center,
                radius = fadeRadius,
                tileMode = TileMode.Clamp
            ),
            blendMode = BlendMode.DstIn
        )
    }
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun trackFullScreen(fullscreen: MutableState<Boolean>)
{

    val state by grooviqCore.controllers.audioController.bassAudioController.state.collectAsState()
    val track = grooviqCore.controllers.audioController.bassQueueController.currentTrack() ?: return
    val prim = MaterialTheme.colorScheme.primary

    val color = remember { if (track.artworkPath == null) prim.toArgb() else dominantColorFromPathStable(track.artworkPath?.pathString ?: "") }

    val focusRequester = remember { FocusRequester() }

    val fftBuf = BassFloatBuffer(1024)  // Can use global, but local for clarity
    val waveBuf = BassFloatBuffer(1024)

    var blobBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(fullscreen.value) {
        while (fullscreen.value) {
            // FFT (with FLOAT for proper magnitudes)
            Bass.INSTANCE.BASS_ChannelGetData(
                grooviqCore.controllers.audioController.bassAudioController.mixer,
                fftBuf.memory,
                BASS_DATA_FFT2048 or Bass.BASS_DATA_FLOAT
            )
            fftBuf.memory.read(0, fftBuf.array, 0, fftBuf.array.size)

            // WAVEFORM (request full 1024 floats: 4096 bytes)
            Bass.INSTANCE.BASS_ChannelGetData(
                grooviqCore.controllers.audioController.bassAudioController.mixer,
                waveBuf.memory,
                Bass.BASS_DATA_FLOAT or (1024 * 4)
            )
            waveBuf.memory.read(0, waveBuf.array, 0, waveBuf.array.size)

            // Process
            blobBytes = fftToBlobBytes(fftBuf.array, waveBuf.array)

            delay(16L)
        }
    }


        Box(Modifier.fillMaxSize().background(Color(20, 20, 20)).zIndex(1f)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                    fullscreen.value = false
                    true
                } else false
            }
        ) {



            if (track.artworkPath != null)
                Box(Modifier.zIndex(3f).fillMaxSize()) {
                    Crossfade(targetState = track.artworkPath, animationSpec = tween(180)) { artworkPath ->
                        artworkAsync(artworkPath,
                            Modifier.fillMaxSize().scale(2f).alpha(0.5f),
                            blurRadius = 60f
                        )
                    }
                }
            else
                Box(Modifier.zIndex(2f).fillMaxSize().background(Color(26, 26, 26))) {

                }



            BarVisualizer(
                audioBytes = blobBytes,
                modifier = Modifier.fillMaxSize().zIndex(2f)
                    .verticalFadeBottomUp(alpha = 0.4f),
                color = Color(color),
                density = 1.0f,
                style = VisualizerStyle.BARS
            )

            Box(Modifier.zIndex(3f).fillMaxSize().padding(32.dp)) {


                Column(Modifier.align(Alignment.Center).padding(bottom = 32.dp)
                    ,
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(Modifier.weight(1f))


                    Box {

                        Crossfade(
                            targetState = track.artworkPath,
                            animationSpec = tween(180)
                        ) { artworkPath ->
                            artworkAsync(
                                artworkPath,
                                Modifier.size(400.dp)
                            )
                        }

                    }



                    Spacer(Modifier.height(24.dp))


                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {

                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = track.artist,
                        color = Color(255, 255, 255, 160),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                    )

                    if (state.audioInfo != null)
                    {
                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier)
                        {
                            Icon(
                                Icons.Sharp.SurroundSound,
                                "",
                                tint = Color(255, 255, 255, 100)
                            )
                            Text(
                                text =
                                    state.audioInfo!!.prettyString()
                                ,
                                color = Color(255, 255, 255, 100),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                    }

                    Spacer(Modifier.weight(1f))


                    Column {

                        Box(Modifier.fillMaxWidth()) {

                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(

                                    modifier = Modifier
                                        .size(40.dp)
                                    ,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                    ),
                                    onClick = {
                                        fullscreen.value = false
                                    }
                                )
                                {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        imageVector = Icons.Sharp.Fullscreen, contentDescription = "",
                                        tint = Color(255, 255, 255, 100)
                                    )
                                }
                            }


                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {

                                IconButton(

                                    modifier = Modifier
                                        .size(40.dp)
                                    ,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                    ),
                                    onClick = {

                                        grooviqCore.controllers.audioController.bassQueueController.movePrev()
                                    }
                                )
                                {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        imageVector = Icons.Sharp.SkipPrevious, contentDescription = "",
                                        tint = Color(255, 255, 255)
                                    )
                                }

                                IconButton(
                                    modifier = Modifier
                                        .size(80.dp)
                                    ,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                    ),
                                    onClick = {

                                        if (state.isPlaying)
                                            grooviqCore.controllers.audioController.bassAudioController.pause()
                                        else
                                            grooviqCore.controllers.audioController.bassAudioController.resume()

                                    }
                                )
                                {
                                    Icon(
                                        modifier = Modifier.size(32.dp),
                                        imageVector = if (state.isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow, contentDescription = "",
                                        tint = Color(255, 255, 255)
                                    )
                                }

                                IconButton(

                                    modifier = Modifier
                                        .size(40.dp)
                                    ,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                    ),
                                    onClick = {

                                        grooviqCore.controllers.audioController.bassQueueController.moveNext()

                                    }
                                )
                                {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        imageVector = Icons.Sharp.SkipNext, contentDescription = "",
                                        tint = Color(255, 255, 255)
                                    )
                                }
                            }


                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {

                                IconButton(

                                    modifier = Modifier
                                        .size(40.dp)
                                    ,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                    ),
                                    onClick = {
                                        grooviqCore.controllers.audioController.bassQueueController
                                            .toggleShuffle(
                                                !grooviqCore.controllers.audioController.
                                                bassQueueController.isShuffle
                                            )
                                    }
                                )
                                {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        imageVector = Icons.Sharp.Shuffle, contentDescription = "",
                                        tint =
                                            if (grooviqCore.controllers.audioController.bassQueueController.isShuffle)
                                                Color(color)
                                            else
                                                Color(255, 255, 255, 100)
                                    )
                                }

                                IconButton(

                                    modifier = Modifier
                                        .size(40.dp)
                                    ,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                    ),
                                    onClick = {
                                        grooviqCore.controllers.audioController.bassQueueController.toggleRepeat()
                                    }
                                )
                                {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        imageVector =
                                            if (grooviqCore.controllers.audioController.bassQueueController.repeatMode == repeatMods.REPEAT_OFF)
                                                Icons.Sharp.Repeat
                                            else if (grooviqCore.controllers.audioController.bassQueueController.repeatMode == repeatMods.REPEAT_ALL)
                                                Icons.Sharp.Repeat
                                            else
                                                Icons.Sharp.RepeatOne
                                        ,
                                        contentDescription = "",
                                        tint =
                                            if (grooviqCore.controllers.audioController.bassQueueController.repeatMode == repeatMods.REPEAT_OFF)
                                                Color(255, 255, 255, 100)
                                            else
                                                Color(color)
                                    )
                                }
                            }

                        }

                        var sliderValue by remember { mutableStateOf(0f) }
                        var isSeeking by remember { mutableStateOf(false) }

                        LaunchedEffect(state.positionSec, isSeeking) {
                            if (!isSeeking) {
                                sliderValue = state.positionSec.toFloat()
                            }
                        }

                        var sliderHovered by remember { mutableStateOf(false) }

                        val hoverAnim by animateFloatAsState(
                            targetValue = if (sliderHovered) 1f else 0f,
                            label = "sliderHover"
                        )

                        val interactionSource = remember { MutableInteractionSource() }
                        var trackWidthPx by remember { mutableStateOf(0) }

                        if (state.durationSec > 0)
                            Slider(
                                interactionSource = interactionSource,
                                value = sliderValue,
                                onValueChange = {
                                    isSeeking = true
                                    sliderValue = it
                                },
                                onValueChangeFinished = {
                                    isSeeking = false
                                    grooviqCore.controllers.audioController.bassAudioController.seek(sliderValue.toDouble())
                                },
                                valueRange = 0f..state.durationSec.toFloat(),
                                modifier = Modifier.fillMaxWidth().zIndex(3f)
                                    .animateContentSize()
                                    .onPointerEvent(PointerEventType.Enter)
                                    {
                                        sliderHovered = true
                                    }
                                    .onPointerEvent(PointerEventType.Exit)
                                    {
                                        sliderHovered = false
                                    }
                                ,
                                track = { sliderState ->

                                    val trackHeight = lerp(2.dp, 5.dp, hoverAnim)
                                    val inactiveAlpha = 0.1f + (0.05f) * hoverAnim
                                    Box(
                                        Modifier.onGloballyPositioned { coords ->
                                            trackWidthPx = coords.size.width
                                        }
                                    )
                                    {
                                        FlatSliderTrack(
                                            sliderState = sliderState,
                                            steps = 0,
                                            height = trackHeight,
                                            colors = SliderDefaults.colors(
                                                inactiveTrackColor = Color(255, 255, 255).copy(alpha = inactiveAlpha),
                                                activeTrackColor = Color(color)
                                            )
                                        )
                                    }

                                },
                                thumb = { state ->

                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(24.dp)
                                            .background(
                                                color = Color(color),
                                                shape = CircleShape
                                            )
                                    )
                                }


                            )
                    }

                }


            }


        }


    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }


}