package ui.screens.rightPager

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Fullscreen
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PermMedia
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Repeat
import androidx.compose.material.icons.sharp.RepeatOne
import androidx.compose.material.icons.sharp.Shuffle
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material.icons.sharp.SurroundSound
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.example.audioindex.AudioFolderController
import org.example.audioindex.ScannedAudio
import org.example.bass.bassController.prettyString
import org.example.bass.queue.repeatMods
import org.example.bassAudioController
import org.example.bassQueueController
import org.example.toTimeString
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.ui.screens.leftPager.queue.drawQueue
import org.example.ui.screens.leftPager.settings.AppPrefs
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
    overlayEnabled: MutableState<Boolean>
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
                    listState
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
                                y = -40
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

                                    Row(verticalAlignment = Alignment.CenterVertically)
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
                                            fontSize = 9.sp,
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


                                        Text(state.positionSec.toTimeString(), fontSize = 11.sp,
                                            color = col
                                        )

                                        Text("  /  ", fontSize = 11.sp, color = Color(255, 255, 255))

                                        Text(state.durationSec.toTimeString(), fontSize = 11.sp,
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
                                                bassQueueController.toggleShuffle(!bassQueueController.isShuffle)
                                            }
                                        )
                                        {
                                            Icon(
                                                modifier = Modifier.size(24.dp),
                                                imageVector = Icons.Sharp.Shuffle, contentDescription = "",
                                                tint =
                                                    if (bassQueueController.isShuffle)
                                                        col
                                                    else
                                                        Color(255, 255, 255, 100)
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
                                                bassQueueController.toggleRepeat()
                                            }
                                        )
                                        {
                                            Icon(
                                                modifier = Modifier.size(24.dp),
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
                                                        col
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
            audioFolderController
                .tracksByAlbum(openedAudioSource.value)
                .sortedWith(
                    compareBy<ScannedAudio>(
                        { it.disc },
                        { it.pos.toIntOrNull() ?: Int.MAX_VALUE }
                    )
                )

        if (openedAlbumTracks.isEmpty())
        {
            openedAudioSource.value = ""
            return@Column
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

                val hasMultipleDiscs =
                    openedAlbumTracks
                        .map { it.disc }
                        .distinct()
                        .size > 1

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

                            Box {
                                Crossfade(
                                    targetState = trackWithArtOrFirst.artworkPath,
                                    animationSpec = tween(180)
                                ) { artworkPath ->
                                    artworkAsync(
                                        artworkPath,
                                        Modifier.fillMaxWidth().blur(60.dp).height(414.dp - 30.dp).alpha(0.3f)
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
                            modifier = Modifier.fillMaxWidth(),
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
                                    modifier = Modifier.padding(start = 16.dp).fillMaxWidth()
                                ) {

                                    Text(item.title, fontSize = 16.sp,
                                        color = if (bassQueueController.isPlaying(item, item.albumKey))
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



            }

        }

    }
}