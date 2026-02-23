package ui.screens.rightPager

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PermMedia
import androidx.compose.material.icons.sharp.Queue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.example.audioindex.AudioFolderController
import org.example.audioindex.ScannedAudio
import org.example.bass.bassController.trackSource
import org.example.bassAudioController
import org.example.bassQueueController
import org.example.dashedBorder
import org.example.folderGetter.PlaylistController
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.ui.screens.leftPager.playlists.dropDownMenuOpenMode
import org.example.ui.screens.leftPager.playlists.openMode
import org.example.ui.screens.leftPager.queue.drawQueue
import org.example.ui.screens.rightPager.tracklist.createDropDownTrack
import org.example.ui.screens.rightPager.bottomBar.drawBottomBar
import org.example.ui.screens.rightPager.tracklist.drawTrackList
import org.example.ui.screens.rightPager.tracklist.placeholders.emptyPlaylistPlaceHolder
import org.example.ui.screens.rightPager.tracklist.placeholders.notSelectedSourcePlaceholder
import org.example.wizui.wizui
import kotlin.io.path.Path

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun renderRightPager(
    audioFolderController: AudioFolderController,
    openedAudioSource: MutableState<String>,
    playlistController: PlaylistController,
    overlayEnabled: MutableState<Boolean>,
)
{
    val openedTab = rememberSaveable { mutableStateOf(1) }

    val hazeState = rememberHazeState()

    val col = MaterialTheme.colorScheme.primary

    val offsetOfBottomBar = rememberSaveable { mutableStateOf(0.dp) }

    val state by bassAudioController.state.collectAsState()


    Box(Modifier.padding()            .drawWithCache {
        onDrawBehind {

            drawLine(
                color = Color(255, 255, 255, 30),
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 2f
            )
        }
    }) {

        val listState = rememberSaveable(
            saver = LazyListState.Saver
        ) {
            LazyListState()
        }


        var previousAlbum by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(openedAudioSource.value) {
            val current = openedAudioSource.value

            if (previousAlbum == null) {
                previousAlbum = current
                return@LaunchedEffect
            }

            if (previousAlbum != current) {
                listState.scrollToItem(0)
                openedTab.value = 1
            }

            previousAlbum = current
        }


        val pagerState = rememberPagerState(
            initialPage = openedTab.value - 1,
            pageCount = { 2 }
        )

        LaunchedEffect(openedTab.value) {
            pagerState.animateScrollToPage(openedTab.value - 1)
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondViewportPageCount = 2,
            modifier = Modifier.fillMaxSize().background(Color(16, 16, 16))
        ) { page ->

            when (page) {

                0 -> drawAlbum(
                    audioFolderController,
                    openedAudioSource,
                    overlayEnabled,
                    hazeState,
                    offsetOfBottomBar,
                    listState,
                    playlistController
                )

                1 -> drawQueue(offsetOfBottomBar)


            }
        }

        Row(Modifier.fillMaxWidth().padding(start =32.dp, end = 32.dp,top = 32.dp, bottom = 32.dp))
        {

            wizui.wizButton(
                contentColor = Color(255, 255, 255, 100),
                contentColorToggled =  Color(255, 255, 255, 255),
                backgroundColor = Color(255, 255, 255, 0),
                turnOffToggleIndication = true,
                modifier = Modifier.weight(1f)
                    .height(50.dp)
                    .hazeEffect(
                        hazeState,
                        style = HazeStyle(
                            backgroundColor = Color(20, 20, 20, 255),
                            blurRadius = 25.dp,
                            tint = (HazeTint(
                                color = Color(10, 10, 10, 30)
                            )),
                            noiseFactor = 0.15f
                        )
                    )
                    .border(1.dp, Color(255, 255, 255, 30)),
                shape = RectangleShape,
                onClick = {
                    openedTab.value = 1
                },
                toggleVariable = openedTab.value == 1
            ) {
                Text("source", fontSize = 16.sp,)
            }

            wizui.wizButton(
                contentColor = Color(255, 255, 255, 100),
                contentColorToggled =  Color(255, 255, 255, 255),
                backgroundColor = Color(255, 255, 255, 0),
                turnOffToggleIndication = true,
                modifier = Modifier.weight(1f).height(50.dp)
                    .hazeEffect(
                        hazeState,
                        style = HazeStyle(
                            backgroundColor = Color(20, 20, 20, 255),
                            blurRadius = 25.dp,
                            tint = (HazeTint(
                                color = Color(10, 10, 10, 30)
                            )),
                            noiseFactor = 0.15f
                        )
                    )
                    .border(1.dp, Color(255, 255, 255, 30)),
                shape = RectangleShape,
                onClick = {
                    openedTab.value = 3
                },
                toggleVariable = openedTab.value == 3
            )
            {
                Text("queue", fontSize = 16.sp,)
            }

        }


        Column(modifier = Modifier
            .align(Alignment.BottomCenter)

        )
        {
            drawBottomBar(
                offsetOfBottomBar,
                state,
                col,
                hazeState,
                openedAudioSource,
                overlayEnabled,

            )

        }





    }
}

fun formatDuration(seconds: Double): String {
    val total = seconds.toLong()

    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun drawAlbum(
    audioFolderController: AudioFolderController,
    openedAudioSource: MutableState<String>,
    overlayEnabled: MutableState<Boolean>,
    hazeState: HazeState,
    offsetOfBottomBar: MutableState<Dp>,
    listState: LazyListState,
    playlistController: PlaylistController,
)
{
    val col = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(Color(16, 16, 16))

    )

    {

        val openedAlbumTracks =

            when (val parsed = trackSource.fromString(openedAudioSource.value)) {

                is trackSource.album ->
                    audioFolderController
                        .tracksByAlbum(openedAudioSource.value)
                        .sortedWith(
                            compareBy<ScannedAudio>(
                                { it.disc },
                                { it.pos.toIntOrNull() ?: Int.MAX_VALUE }
                            )
                        )

                is trackSource.playlist ->
                    playlistController.tracksByPlaylist(parsed.playlistId)

                else -> { emptyList()}
            }


        LaunchedEffect(openedAudioSource.value) {

            when (val parsed = trackSource.fromString(openedAudioSource.value)) {

                is trackSource.album -> {
                    if (!playlistController.db.hasAlbumKey(parsed.albumKey)) {
                        openedAudioSource.value = ""
                    }
                }


                is trackSource.playlist -> {
                    if (!playlistController.db.hasPlaylistId(parsed.playlistId)) {
                        openedAudioSource.value = ""
                    }
                }


                else -> {

                }
            }
        }

        if (openedAudioSource.value.isBlank())
        {
            notSelectedSourcePlaceholder()
        }
        else if (openedAlbumTracks.isEmpty()) {

            val parsed = trackSource.fromString(openedAudioSource.value)
            val isAlbum = parsed is trackSource.album

            if (!isAlbum) {

                emptyPlaylistPlaceHolder(
                    openedAudioSource   = openedAudioSource,
                    playlistController  = playlistController
                )

            }
            else {
                openedAudioSource.value = ""
            }

        }
        else
        {

            Box(Modifier.fillMaxSize().background(Color(16, 16, 16))) {

                //tracklist draw
                drawTrackList(
                    openedAlbumTracks = openedAlbumTracks,
                    listState = listState,
                    hazeState = hazeState,
                    openedAudioSource = openedAudioSource,
                    offsetOfBottomBar = offsetOfBottomBar
                )


            }

        }

    }
}