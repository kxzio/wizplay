package ui.screens.rightPager

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PermMedia
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.example.audioindex.AudioFolderController
import org.example.bass.bassController.playlistItem
import org.example.bassAudioController
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.wizui.wizui
import org.example.wizui.wizui.FlatSliderTrack

fun formatTime(sec: Double): String {
    val s = sec.toInt()
    val m = s / 60
    val r = s % 60
    return "%d:%02d".format(m, r)
}

fun Modifier.bottomGradient(col: Color) = this.drawWithCache {
    val gradient = Brush.radialGradient(
        colors = listOf(
            col.copy(alpha = 0.10f),
            Color.Transparent
        ),
        center = Offset(size.width / 2f, size.height),
        radius = size.width * 0.5f
    )

    val strokeWidth = 1.dp.toPx()
    val y = strokeWidth / 2

    onDrawBehind {
        drawRect(gradient)
    }
}

fun Modifier.topHalfCircleBorder(
    color: Color,
    strokeWidth: Dp
) = this.drawWithCache {

    val strokePx = strokeWidth.toPx()

    onDrawWithContent {
        drawContent()

        drawArc(
            color = color,
            startAngle = 180f,      // слева
            sweepAngle = 180f,      // верхняя половина
            useCenter = false,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round // красиво закругляет края
            ),
            size = Size(
                size.width - strokePx,
                size.height - strokePx
            ),
            topLeft = Offset(
                strokePx / 2,
                strokePx / 2
            )
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun renderRightPager(
    audioFolderController: AudioFolderController,
    openedAudioSource: MutableState<String>,
)
{
    val hazeState = rememberHazeState()

    val col = MaterialTheme.colorScheme.primary

    val offsetOfBottomBar = remember { mutableStateOf(0.dp) }

    val state by bassAudioController.state.collectAsState()

    wizui.wizColumn(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(Color(16, 16, 16))
            .drawWithCache {
                onDrawBehind {

                    drawLine(
                        color = Color(255, 255, 255, 30),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2f
                    )
                }
            }
    )

    {

        val openedAlbumTracks = audioFolderController.tracksByAlbum(openedAudioSource.value)
            .sortedBy { (if (it.pos != "") it.pos.toInt() else 0) }

        if (openedAlbumTracks.isEmpty())
        {
            openedAudioSource.value = ""
            return@wizColumn
        }

        if (openedAudioSource.value.isBlank())
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {

                Icon(Icons.Sharp.PermMedia, "",
                    tint = Color(255, 255, 255, 30),
                    modifier = Modifier.size(150.dp)
                )

                Text("select album or playlist from the media-tab", fontSize = 16.sp, color = Color(255, 255, 255))
            }
        else
        {

            Box(Modifier.fillMaxSize().background(Color(16, 16, 16))) {

                var trackWithArtOrFirst = openedAlbumTracks.firstOrNull { it.artworkPath != null }
                if (trackWithArtOrFirst == null)
                    trackWithArtOrFirst = openedAlbumTracks.first()


                LazyColumn(Modifier
                    .padding(0.dp)
                    .background(Color(16, 16, 16))
                    .hazeSource(hazeState)
                ) {

                    item {

                        Box {

                            Box {
                                Crossfade(
                                    targetState = trackWithArtOrFirst.artworkPath,
                                    animationSpec = tween(180)
                                ) { artworkPath ->
                                    artworkAsync(
                                        artworkPath,
                                        Modifier.fillMaxWidth().blur(60.dp).height(314.dp).alpha(0.3f)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding())
                            {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(32.dp)) {

                                    Box(Modifier.size(250.dp)) {

                                        Crossfade(
                                            targetState = trackWithArtOrFirst.artworkPath,
                                            animationSpec = tween(180)
                                        ) { artworkPath ->
                                            artworkAsync(
                                                artworkPath,
                                                Modifier.size(250.dp)
                                            )
                                        }
                                    }

                                    Column {

                                        Text(
                                            trackWithArtOrFirst.album,
                                            fontSize = 22.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color(255, 255, 255)
                                        )

                                        Spacer(Modifier.height(4.dp))


                                        Text(
                                            trackWithArtOrFirst.artist,
                                            fontSize = 16.sp,
                                            color = Color(255, 255, 255, 120)
                                        )

                                        Spacer(Modifier.height(4.dp))

                                        Text(
                                            trackWithArtOrFirst.year,
                                            fontSize = 16.sp,
                                            color = Color(255, 255, 255, 100)
                                        )
                                    }

                                }


                                HorizontalDivider(
                                    modifier = Modifier.padding().fillMaxWidth(),
                                    thickness = 1.0.dp,
                                    color = Color(255, 255, 255, 60)
                                )

                                Spacer(Modifier.height(18.dp))


                            }
                        }
                    }

                    itemsIndexed(openedAlbumTracks) { num, item ->
                        wizui.wizButton(
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth(),
                            contentColor = Color(255, 255, 255),
                            backgroundColor = Color(35, 35, 35, 0),
                            onClick = {

                                val state = bassAudioController.state.value

                                val sameAlbum =
                                    state.playlist.isNotEmpty() &&
                                            state.playlist.first().audioSource == item.albumKey

                                val sameTrack =
                                    sameAlbum &&
                                            state.index == openedAlbumTracks.indexOf(item)

                                if (!sameAlbum || !sameTrack) {
                                    bassAudioController.playQueue(
                                        list = openedAlbumTracks.map { track ->
                                            playlistItem(
                                                track = track,
                                                audioSource = track.albumKey
                                            )
                                        },
                                        startIndex = openedAlbumTracks.indexOf(item)
                                    )
                                }
                            }
                        ) {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Box(
                                    modifier = Modifier
                                        .width(32.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = (num + 1).toString(),
                                        fontSize = 14.sp,
                                        color = Color(255, 255, 255, 100),
                                        textAlign = TextAlign.End,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    modifier = Modifier.padding(start = 16.dp).fillMaxWidth()
                                ) {

                                    Text(item.title, fontSize = 16.sp,
                                        color = if (state.isPlayingItem(item))
                                            MaterialTheme.colorScheme.primary else Color.White)

                                    Spacer(Modifier.height(4.dp))
                                    Text(item.artist, fontSize = 12.sp, color = Color(255, 255, 255, 100))
                                }
                            }

                        }
                    }

                    item {
                        Spacer(Modifier.height(offsetOfBottomBar.value + 16.dp))
                    }
                }


                Column(modifier = Modifier
                    .align(Alignment.BottomCenter)

                )
                {
                    val track = state.playlist.getOrNull(state.index)

                    var realHeight by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current

                    if (track != null) {

                        Box {

                            /* ───── ПРОГРЕСС ───── */

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

                            Slider(
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
                                    FlatSliderTrack(
                                        sliderState = sliderState,
                                        steps = 0,
                                        height = trackHeight,
                                        colors = SliderDefaults.colors(
                                            inactiveTrackColor = Color(120, 120, 120).copy(alpha = inactiveAlpha),
                                            activeTrackColor = col
                                        )
                                    )
                                },
                                thumb = {
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
                                                color = Color(100, 100, 100, 20)
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
                                Column(Modifier.zIndex(3f)) {

                                    /* ───── ТРЕК ───── */

                                    Text(
                                        text = track!!.track.title,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = track!!.track.artist,
                                        color = Color(255, 255, 255, 100),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                }
                            }

                            Row(Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).zIndex(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                //buttons controls

                                IconButton(

                                    modifier = Modifier
                                        .size(40.dp)
                                    ,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                    ),
                                    onClick = {

                                        bassAudioController.prev()

                                    }
                                )
                                {
                                    Icon(
                                        modifier = Modifier.size(30.dp),
                                        imageVector = Icons.Sharp.SkipPrevious, contentDescription = "",
                                        tint = Color(255, 255, 255)
                                    )
                                }

                                Spacer(Modifier.width(16.dp))


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
                                        modifier = Modifier.size(50.dp),
                                        imageVector = if (state.isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow, contentDescription = "",
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

                                        bassAudioController.next()

                                    }
                                )
                                {
                                    Icon(
                                        modifier = Modifier.size(30.dp),
                                        imageVector = Icons.Sharp.SkipNext, contentDescription = "",
                                        tint = Color(255, 255, 255)
                                    )
                                }
                            }

                        }

                    }
                    else {
                        offsetOfBottomBar.value = 0.dp
                    }

                }

            }

        }

    }
}