package org.example.ui.screens.rightPager.bottomBar

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.example.bass.bassController.PlayerState
import org.example.bass.bassController.prettyString
import org.example.bass.queue.repeatMods
import org.example.bassAudioController
import org.example.bassQueueController
import org.example.bottomGradient
import org.example.toTimeString
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.ui.uiHelpers.wizuiUIMove
import org.example.wizui.wizui.FlatSliderTrack

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun drawBottomBar(
    offsetOfBottomBar: MutableState<Dp>,
    state: PlayerState,
    col: Color,
    hazeState: HazeState,
    openedAudioSource: MutableState<String>,
    overlayEnabled: MutableState<Boolean>
)
{
    Box(modifier = Modifier)
    {
        val track = bassQueueController.currentTrack()

        var realHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        if (track != null) {

            Box {

                var sliderValue by remember { mutableStateOf(0f) }
                var isSeeking by remember { mutableStateOf(false) }

                LaunchedEffect(state.positionSec, isSeeking) {
                    if (!isSeeking) {
                        sliderValue = state.positionSec.toFloat()
                    }
                }

                Box(
                    modifier = Modifier
                        .zIndex(2f)
                        .matchParentSize()
                        .bottomGradient(col)
                )

                var sliderHovered by remember { mutableStateOf(false) }

                val hoverAnim by animateFloatAsState(
                    targetValue = if (sliderHovered) 1f else 0f,
                    label = "sliderHover"
                )

                val thumbAlpha by animateFloatAsState(
                    targetValue = if (sliderHovered) 1f else 0f,
                    animationSpec = tween(120),
                    label = "thumbAlpha"
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isDragging by interactionSource.collectIsDraggedAsState()
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
                            bassAudioController.seek(sliderValue.toDouble())
                        },
                        valueRange = 0f..state.durationSec.toFloat(),
                        modifier = Modifier.fillMaxWidth().zIndex(3f)
                            .offset(y = -21.dp)
                            .align(Alignment.TopCenter)
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
                            val inactiveAlpha = 0.1f + (0.35f - 0.1f) * hoverAnim
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
                                        inactiveTrackColor = Color(120, 120, 120).copy(alpha = inactiveAlpha),
                                        activeTrackColor = col
                                    )
                                )
                            }

                        },
                        thumb = { state ->

                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(32.dp)
                                    .graphicsLayer {
                                        alpha = 0f
                                    }
                                    .background(
                                        color = col,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }


                    )

                val density = LocalDensity.current

                if (isDragging) {

                    val fraction =
                        sliderValue / state.durationSec.toFloat()

                    val thumbX =
                        (trackWidthPx * fraction).toInt()

                    val offset = with(LocalDensity.current) {
                        IntOffset(
                            x = thumbX - 26,
                            y = -45
                        )
                    }


                    Popup(
                        alignment = Alignment.TopStart,
                        offset = offset,
                    ) {
                        TimePreviewBubble(
                            text = sliderValue.toDouble().toTimeString()
                        )
                    }
                }

                //bottom bar
                Column(
                    modifier = Modifier
                        .zIndex(1f)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                        .hazeEffect(
                            hazeState,
                            style = HazeStyle(
                                backgroundColor = Color(25, 25, 25),
                                blurRadius = 25.dp,
                                tint = (HazeTint(
                                    color = Color(10, 10, 10, 150)
                                )),
                                noiseFactor = 0.15f
                            )
                        )
                        .background(Color(0, 0, 0, 30))
                        .drawWithCache {

                            val strokeWidth = 1.dp.toPx()
                            val y = 0f + strokeWidth / 2

                            onDrawBehind {
                                drawLine(
                                    color = Color(255, 255, 255, 30), start = Offset(0f, y),
                                    end = Offset(size.width, y), strokeWidth = strokeWidth
                                )
                            }

                        }
                        .onSizeChanged { size ->
                            realHeight = with(density) { size.height.toDp() }
                            offsetOfBottomBar.value = realHeight
                        }
                        .padding(32.dp)
                )
                {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {

                        IconButton(
                            modifier = Modifier
                                .size(80.dp)
                            ,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                            ),
                            onClick = {

                                if (state.isPlaying)
                                    bassAudioController.pause()
                                else
                                    bassAudioController.resume()

                            }
                        )
                        {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                imageVector = if (state.isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow, contentDescription = "",
                                tint = Color(255, 255, 255)
                            )
                        }

                        Spacer(Modifier.width(32.dp))


                        Column(Modifier.zIndex(3f).weight(1f).padding(end = 8.dp)
                        ) {

                            /* ───── ТРЕК ───── */


                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    wizuiUIMove.albumListMoveToAlbumKey = track.albumKey
                                    openedAudioSource.value = track.albumKey
                                    AppPrefs.setString("openedAudioSource", track.albumKey) }
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = track.artist,
                                color = Color(255, 255, 255, 160),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (state.audioInfo != null)
                            {
                                Spacer(Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = -0.dp))
                                {

                                    Icon(
                                        imageVector = Icons.Sharp.Shuffle, contentDescription = "",
                                        tint =
                                            if (bassQueueController.isShuffle)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                Color(255, 255, 255, 100),
                                        modifier = Modifier.size(20.dp).clickable {
                                            bassQueueController.toggleShuffle(!bassQueueController.isShuffle)
                                        }
                                    )

                                    VerticalDivider(thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp).height(16.dp))

                                    Icon(
                                        imageVector =
                                            if (bassQueueController.repeatMode == repeatMods.REPEAT_OFF)
                                                Icons.Sharp.Repeat
                                            else if (bassQueueController.repeatMode == repeatMods.REPEAT_ALL)
                                                Icons.Sharp.Repeat
                                            else
                                                Icons.Sharp.RepeatOne
                                        ,
                                        contentDescription = "",
                                        tint =
                                            if (bassQueueController.repeatMode == repeatMods.REPEAT_OFF)
                                                Color(255, 255, 255, 100)
                                            else
                                                MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp).clickable {
                                            bassQueueController.toggleRepeat()
                                        }
                                    )


                                    VerticalDivider(thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp).height(16.dp))

                                    Icon(
                                        Icons.Sharp.SurroundSound,
                                        "",
                                        tint = Color(255, 255, 255, 100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text =
                                            state.audioInfo!!.format.toString()
                                        ,
                                        color = Color(255, 255, 255, 100),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                            }



                        }

                        Row(Modifier.zIndex(1f)) {

                            Column()
                            {

                            }

                            Column(Modifier.zIndex(1f), horizontalAlignment = Alignment.CenterHorizontally) {

                                //buttons controls

                                Row(Modifier.zIndex(2f).align(Alignment.End).padding(end = 6.dp)) {


                                    Text(state.positionSec.toTimeString(), fontSize = 13.sp,
                                        color = col
                                    )

                                    Text("  /  ", fontSize = 13.sp, color = Color(255, 255, 255))

                                    Text(state.durationSec.toTimeString(), fontSize = 13.sp,
                                        color = Color(255, 255, 255))
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {

                                    IconButton(

                                        modifier = Modifier
                                            .size(40.dp)
                                        ,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                        ),
                                        onClick = {
                                            overlayEnabled.value = true
                                        }
                                    )
                                    {
                                        Icon(
                                            modifier = Modifier.size(24.dp),
                                            imageVector = Icons.Sharp.Fullscreen, contentDescription = "",
                                            tint = Color(255, 255, 255, 100)
                                        )
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    IconButton(

                                        modifier = Modifier
                                            .size(40.dp)
                                        ,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                        ),
                                        onClick = {

                                            bassQueueController.movePrev()
                                        }
                                    )
                                    {
                                        Icon(
                                            modifier = Modifier.size(24.dp),
                                            imageVector = Icons.Sharp.SkipPrevious, contentDescription = "",
                                            tint = Color(255, 255, 255)
                                        )
                                    }


                                    Spacer(Modifier.width(16.dp))

                                    IconButton(

                                        modifier = Modifier
                                            .size(40.dp)
                                        ,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                        ),
                                        onClick = {

                                            bassQueueController.moveNext()

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

                            }
                        }


                    }

                }




            }

        }
        else {
            offsetOfBottomBar.value = 0.dp
        }

    }
}