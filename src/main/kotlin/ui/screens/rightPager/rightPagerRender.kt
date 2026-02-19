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
import androidx.compose.material.icons.sharp.DeleteForever
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PermMedia
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Queue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
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
import org.example.ui.screens.leftPager.queue.drawQueue
import org.example.ui.screens.rightPager.drawBottomBar
import org.example.wizui.wizui
import kotlin.io.path.Path

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

@Composable
fun TimePreviewBubble(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(20, 20, 20),
            )
            .border(
                width = 1.dp,
                color = Color(255, 255, 255, 30),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
        )
    }
}


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
                                color = Color(100, 100, 100, 20)
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
                                color = Color(100, 100, 100, 20)
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


        val albumDurationCache = remember { mutableMapOf<String, String>() }

        val currentAlbumDuration by produceState<String>(
            initialValue = "–",
            key1 = openedAudioSource.value
        ) {
            value = albumDurationCache.getOrPut(openedAudioSource.value) {
                formatDuration(
                    bassAudioController.getAlbumDurationSec(openedAlbumTracks)
                )
            }
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
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {

                Icon(Icons.Sharp.PermMedia, "",
                    tint = Color(255, 255, 255, 30),
                    modifier = Modifier.size(150.dp)
                )

                Text("select album or playlist from the media-tab", fontSize = 16.sp, color = Color(255, 255, 255))
            }
        else if (openedAlbumTracks.isEmpty()) {

            val parsed = trackSource.fromString(openedAudioSource.value)
            val isAlbum = parsed is trackSource.album

            if (!isAlbum) {

                Box(Modifier.fillMaxSize().padding(bottom = 142.dp)) {
                    Box {

                        Column(modifier = Modifier.padding(top = 76.dp))
                        {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(
                                    top = 32.dp,
                                    start = 32.dp,
                                    end = 32.dp,
                                    bottom = 26.dp
                                )) {

                                Box(Modifier.size(250.dp)) {

                                    Crossfade(
                                        targetState = Path(""),
                                        animationSpec = tween(180)
                                    ) { artworkPath ->
                                        artworkAsync(
                                            artworkPath,
                                            Modifier.size(250.dp)
                                        )
                                    }
                                }


                                Column {

                                    val parsed = trackSource.fromString(openedAudioSource.value)
                                    val playlistId = if (parsed is trackSource.playlist) {
                                        parsed.playlistId
                                    } else 0

                                    val playlist = playlistController.getPlaylistById(playlistId)

                                    Text(
                                        playlist?.name ?: "playlist name",
                                        fontSize = 28.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color(255, 255, 255)
                                    )

                                    Spacer(Modifier.height(6.dp))


                                    Text(
                                        "playlist",
                                        fontSize = 16.sp,
                                        color = Color(255, 255, 255, 120)
                                    )

                                    Spacer(Modifier.height(6.dp))


                                }

                            }

                            HorizontalDivider(
                                modifier = Modifier.padding().fillMaxWidth(),
                                thickness = 1.0.dp,
                                color = Color(255, 255, 255, 60)
                            )

                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
                            {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                    Icon(Icons.Sharp.Queue, "",
                                        tint = Color(255, 255, 255, 30),
                                        modifier = Modifier.size(150.dp)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                                        Text("this source is empty, ", fontSize = 16.sp, color = Color(255, 255, 255))

                                        wizui.wizButton(
                                            backgroundColor = Color(0, 0, 0, 0),
                                            modifier = Modifier.border(
                                                BorderStroke(0.5.dp, Color(255, 255, 255, 100))),
                                            contentColor = Color.White,
                                            shape = RectangleShape,
                                            onClick = {

                                            },
                                        ){
                                            Text("add something!")
                                        }

                                    }

                                }
                            }


                        }
                    }
                }
            }
            else {
                openedAudioSource.value = ""
            }

        }
        else
        {

            Box(Modifier.fillMaxSize().background(Color(16, 16, 16))) {

                var trackWithArtOrFirst = openedAlbumTracks.firstOrNull { it.artworkPath != null }
                if (trackWithArtOrFirst == null)
                    trackWithArtOrFirst = openedAlbumTracks.firstOrNull()

                val hasMultipleDiscs =
                    openedAlbumTracks
                        .map { it.disc }
                        .distinct()
                        .size > 1

                val trackDropDownOpenPath = remember { mutableStateOf("") }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                    .padding(0.dp)
                    .background(Color(16, 16, 16))
                    .hazeSource(hazeState),
                    contentPadding = PaddingValues()
                ) {


                    item {

                        Box {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(414.dp - 30.dp)
                            ) {
                                Crossfade(
                                    targetState = trackWithArtOrFirst?.artworkPath,
                                    animationSpec = tween(180),
                                    modifier = Modifier.fillMaxSize()
                                ) { artworkPath ->
                                    artworkAsync(
                                        artworkPath,
                                        Modifier
                                            .fillMaxSize()
                                            .alpha(0.3f),
                                        blurRadius = 60f
                                    )
                                }
                            }


                            Column(modifier = Modifier.padding(top = 76.dp))
                            {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(
                                        top = 32.dp,
                                        start = 32.dp,
                                        end = 32.dp,
                                        bottom = 26.dp
                                    )) {

                                    Box(Modifier.size(250.dp)) {

                                        Crossfade(
                                            targetState = trackWithArtOrFirst?.artworkPath,
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
                                            trackWithArtOrFirst?.album ?: "",
                                            fontSize = 28.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color(255, 255, 255)
                                        )

                                        Spacer(Modifier.height(6.dp))


                                        Text(
                                            ("· " + trackWithArtOrFirst?.artist) ?: "",
                                            fontSize = 16.sp,
                                            color = Color(255, 255, 255, 120)
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        Text(
                                            trackWithArtOrFirst?.year ?: "",
                                            fontSize = 16.sp,
                                            color = Color(255, 255, 255, 100)
                                        )

                                        Spacer(Modifier.height(6.dp))


                                    }

                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding().fillMaxWidth(),
                                    thickness = 1.0.dp,
                                    color = Color(255, 255, 255, 60)
                                )

                                Spacer(Modifier.height(18.dp))

                                Text(
                                    "-  length : $currentAlbumDuration",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 32.dp, top = 16.dp),
                                    color = Color(255, 255, 255, 60)
                                )

                                Spacer(Modifier.height(18.dp))


                            }
                        }
                    }

                    itemsIndexed(openedAlbumTracks) { num, item ->

                        val isFirstTrack = num == 0
                        val prevDisc = openedAlbumTracks.getOrNull(num - 1)?.disc

                        if (hasMultipleDiscs && (isFirstTrack || item.disc != prevDisc)) {

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 24.dp)) {

                                HorizontalDivider(color = Color(255, 255, 255,60),
                                    modifier = Modifier.width(60.dp).padding(end = 16.dp)
                                )

                                Icon(Icons.Sharp.Album, "", tint = Color(255, 255, 255))

                                Text(
                                    text = "disc ${item.disc}",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                HorizontalDivider(color = Color(255, 255, 255, 60),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        wizui.wizButton(
                            shape = RectangleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPointerEvent(PointerEventType.Press) { event ->
                                    if (event.buttons.isSecondaryPressed) {
                                        trackDropDownOpenPath.value = item.path.toString()
                                    }
                                }.dashedBorder(
                                    1.dp,
                                    color = if (trackDropDownOpenPath.value == item.path.toString())
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else Color(0, 0, 0, 0)
                                ),
                            contentColor = Color(255, 255, 255),
                            backgroundColor = Color(35, 35, 35, 0),
                            onClick = {
                                bassQueueController.buildFromSource(
                                    tracks = openedAlbumTracks,
                                    audioSource = openedAudioSource.value,
                                    startTrack = item
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
                                    modifier = Modifier.padding(start = 16.dp).weight(1f)
                                ) {

                                    Text(item.title, fontSize = 16.sp,
                                        color = if (bassQueueController.isPlaying(item, item.albumKey))
                                            MaterialTheme.colorScheme.primary else Color.White)

                                    Spacer(Modifier.height(4.dp))
                                    Text(item.artist, fontSize = 12.sp, color = Color(255, 255, 255, 100))
                                }

                                Box {

                                    IconButton(onClick = {
                                        trackDropDownOpenPath.value = item.path.toString()
                                    })
                                    {
                                        Icon(Icons.Sharp.MoreVert, "",
                                            tint = Color(255, 255, 255, 100))
                                    }

                                    DropdownMenu(
                                        shape = RectangleShape,
                                        containerColor = Color(20, 20, 20),
                                        border = BorderStroke(0.5.dp, Color(255, 255, 255, 50)),
                                        expanded = trackDropDownOpenPath.value == item.path.toString(),
                                        onDismissRequest = { trackDropDownOpenPath.value = "" },
                                        modifier = Modifier
                                            .width(220.dp).padding(horizontal = 8.dp)
                                    ) {

                                        DropdownMenuItem(
                                            text = { Text("open") },
                                            onClick = {

                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("rename") },
                                            onClick = {

                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                        DropdownMenuItem(
                                            text = {

                                                Row(verticalAlignment = Alignment.CenterVertically) {

                                                    Icon(Icons.Sharp.DeleteForever, "",
                                                        tint = Color(226, 80, 80, 255))

                                                    Text(
                                                        "delete",
                                                        modifier = Modifier.padding(start = 12.dp),
                                                        color = Color(226, 80, 80, 255)
                                                    )
                                                }
                                            },
                                            onClick = {

                                            }
                                        )
                                    }
                                }

                            }

                        }
                    }

                    item {
                        Spacer(Modifier.height(offsetOfBottomBar.value + 16.dp))
                    }
                }



            }

        }

    }
}