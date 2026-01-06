package ui.screens.rightPager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.sharp.PermMedia
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.example.audioindex.AudioFolderController
import org.example.bass.isPlaying
import org.example.bass.playlistItem
import org.example.bassAudioController
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.wizui.wizui
import org.example.wizui.wizui.FlatSliderTrack
import ui.uiHelpers.relativeLetterSpacing

private fun formatTime(sec: Double): String {
    val s = sec.toInt()
    val m = s / 60
    val r = s % 60
    return "%d:%02d".format(m, r)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun renderRightPager(audioFolderController: AudioFolderController, openedAudioSource: MutableState<String>)
{

    val col = MaterialTheme.colorScheme.primary

    val offsetOfBottomBar = remember { mutableStateOf(0.dp) }

    wizui.wizColumn(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(Color(16, 16, 16))
            .drawWithCache {
                onDrawBehind {

                    drawLine(
                        color = Color(255, 255, 255, 300),
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
            val state by bassAudioController.state.collectAsState()

            Box(Modifier.fillMaxSize().background(Color(16, 16, 16))) {

                var trackWithArtOrFirst = openedAlbumTracks.firstOrNull { it.artworkPath != null }
                if (trackWithArtOrFirst == null)
                    trackWithArtOrFirst = openedAlbumTracks.first()

                val hazeState = rememberHazeState()

                LazyColumn(Modifier
                    .padding(16.dp)
                    .background(Color(16, 16, 16))
                    .hazeSource(hazeState)) {

                    item {

                        Column(modifier = Modifier.padding(start = 16.dp))
                        {
                            Spacer(Modifier.height(8.dp))

                            Box(Modifier.size(250.dp)) {
                                artworkAsync(
                                    trackWithArtOrFirst.artworkPath,
                                    Modifier.size(250.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                trackWithArtOrFirst.album,
                                fontSize = 22.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(255, 255, 255)
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                trackWithArtOrFirst.artist,
                                fontSize = 16.sp,
                                color = Color(255, 255, 255, 120)
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                if (trackWithArtOrFirst.year.isEmpty()) "Year not specified" else trackWithArtOrFirst.year,
                                fontSize = 16.sp,
                                color = Color(255, 255, 255, 70)
                            )

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                thickness = 1.0.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    itemsIndexed(openedAlbumTracks) { num, item ->
                        wizui.wizButton(
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth(),
                            contentColor = Color(255, 255, 255),
                            backgroundColor = Color(35, 35, 35, 0),
                            onClick = {

                                bassAudioController.playQueue(

                                    tracks =
                                        openedAlbumTracks.map { track ->
                                            playlistItem(
                                                track = track,
                                                audioSource = track.albumKey
                                            )
                                        },

                                    startIndex = openedAlbumTracks.indexOf(item)
                                )
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
                                        color = if (state.isPlaying(item, item.albumKey))
                                            MaterialTheme.colorScheme.primary else Color.White)

                                    Spacer(Modifier.height(4.dp))
                                    Text(item.artist, fontSize = 12.sp, color = Color(255, 255, 255, 100))
                                }
                            }

                        }
                    }

                    item {
                        Spacer(Modifier.height(offsetOfBottomBar.value))
                    }
                }

                Column(modifier = Modifier
                    .align(Alignment.BottomCenter))
                {
                    val track = state.playlist.getOrNull(state.index)

                    var realHeight by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current

                    if (track != null) {

                        Column(
                            modifier = Modifier
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
                                    val gradient = Brush.radialGradient(
                                        colors = listOf(
                                            col.copy(alpha = 0.17f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width / 2f, size.height),
                                        radius = size.width * 0.5f
                                    )

                                    onDrawBehind {
                                        drawRect(gradient)
                                    }
                                }
                                .onSizeChanged { size ->
                                    realHeight = with(density) { size.height.toDp() }
                                    offsetOfBottomBar.value = realHeight
                                }
                                .padding(16.dp)
                        )
                        {
                            Column {

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

                                Spacer(Modifier.height(6.dp))

                                /* ───── ПРОГРЕСС ───── */

                                var sliderValue by remember { mutableStateOf(0f) }
                                var isSeeking by remember { mutableStateOf(false) }

                                LaunchedEffect(state.positionSec, isSeeking) {
                                    if (!isSeeking) {
                                        sliderValue = state.positionSec.toFloat()
                                    }
                                }

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
                                    modifier = Modifier.fillMaxWidth(),
                                    track = {
                                        FlatSliderTrack(
                                            steps = 0,
                                            sliderState = it,
                                            colors = SliderDefaults.colors(
                                                inactiveTrackColor = Color(255, 255, 255, 30)
                                            )
                                        )
                                    },
                                )

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        formatTime(state.positionSec),
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        formatTime(state.durationSec),
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }

                                return@Box

                                Spacer(Modifier.height(8.dp))

                                /* ───── КНОПКИ ───── */

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    IconButton(onClick = { bassAudioController.prev() }) {
                                        Icon(
                                            Icons.Default.SkipPrevious,
                                            contentDescription = "Prev",
                                            tint = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (state.isPlaying) bassAudioController.pause()
                                            else bassAudioController.resume()
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            if (state.isPlaying)
                                                Icons.Default.Pause
                                            else
                                                Icons.Default.PlayArrow,
                                            contentDescription = "PlayPause",
                                            tint = Color.White,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    IconButton(onClick = { bassAudioController.next() }) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = "Next",
                                            tint = Color.White
                                        )
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

        }

    }
}