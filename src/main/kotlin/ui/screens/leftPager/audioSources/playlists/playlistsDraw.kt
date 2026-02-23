package org.example.ui.screens.leftPager.playlists

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Create
import androidx.compose.material.icons.sharp.Folder
import androidx.compose.material.icons.sharp.LastPage
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PlaylistAddCheckCircle
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.example.audioindex.AudioFolderController
import org.example.dashedBorder
import org.example.folderGetter.Playlist
import org.example.folderGetter.PlaylistController
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.ui.screens.searchingAlgorithm.matchesQueryPlaylist
import org.example.ui.uiHelpers.AniJinPopup
import org.example.ui.uiHelpers.wizuiUIMove
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf
import ui.screens.leftPager.audioSources.playlists.popUps.handlePlaylistPopUps
import ui.screens.leftPager.audioSources.shared.ScrollProgressThumb
import ui.screens.leftPager.audioSources.shared.rememberScrollFraction


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun playlistsWithAlphabetScroller(
    results: MutableList<Playlist>,
    listState: LazyListState,
    highlitedPlaylist1: MutableState<String>,
    openedAudioSource: MutableState<String>,
    deletePlaylistId: MutableState<String>,
    renamePlaylistId: MutableState<String>
) {

    val scope = rememberCoroutineScope()
    val scrollFraction = rememberScrollFraction(listState)

    val highlight = remember { Animatable(0f) }

    val playlistDropDownMenuOpened = remember { dropDownMenuOpenMode() }


    LaunchedEffect(highlitedPlaylist1.value) {
        val target = highlitedPlaylist1.value
        if (target.isEmpty()) return@LaunchedEffect

        // 🔑 ЖДЁМ, пока элемент реально появится в списке
        snapshotFlow { results.size }
            .first {
                results.any { it.name == target }
            }

        val index = results.indexOfFirst { it.name == target }
        if (index != -1) {
            listState.scrollToItem(0)
        }

        highlight.snapTo(0f)

        highlight.animateTo(
            1f,
            tween(400, easing = FastOutLinearInEasing)
        )

        highlight.animateTo(
            0f,
            tween(600, easing = LinearOutSlowInEasing)
        )

        highlitedPlaylist1.value = ""
    }

    val primary = MaterialTheme.colorScheme.primary
    val highlitedColor = MaterialTheme.colorScheme.primary.copy(alpha = highlight.value)

    LaunchedEffect(Unit)
    {
        snapshotFlow { wizuiUIMove.playlistListMoveToPlaylistKey }
            .filterNotNull()
            .collect { key ->

                val index = results.indexOfFirst { it.playlistKey == key }

                if (index != -1)
                    listState.animateScrollToItem(index)

                wizuiUIMove.playlistListMoveToPlaylistKey = null
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ───── YOUR LazyColumn ─────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 69.dp, bottom = 16.dp)
        ) {


            items(items = results, key = { it.playlistKey }, contentType = { "playlist" }) { item ->

                HorizontalDivider(Modifier.fillMaxWidth()
                    .padding(vertical = 0.dp, horizontal = 64.dp), thickness = 1.dp,
                    color = Color(255, 255, 255, 10))

                BoxWithConstraints(Modifier.animateItem()
                    .onPointerEvent(PointerEventType.Press) { event ->
                        if (event.buttons.isSecondaryPressed)
                        {
                            val pos = event.changes.first().position
                            val intOffset = IntOffset(pos.x.toInt(), pos.y.toInt())

                            playlistDropDownMenuOpened.openDropDown(
                                openIndexName = item.id.toString(),
                                offset = intOffset,
                                openModeForSource = openMode.CURSOR_OPENED
                            )

                        }}
                ) {

                    val density = LocalDensity.current
                    val offsetFromIntToDp = with (density) {
                        DpOffset(x = playlistDropDownMenuOpened.intOffset.x.toDp(),
                            y = 0.dp
                        )
                    }

                    createDropDownPlaylist(
                        offsetFromIntToDp = offsetFromIntToDp,
                        expanded =
                            playlistDropDownMenuOpened.openedIndexName == item.id.toString() &&
                            playlistDropDownMenuOpened.open == openMode.CURSOR_OPENED
                        ,
                        onDismissRequest = { playlistDropDownMenuOpened.closeDropDown() },
                        onRename = { renamePlaylistId.value = item.id.toString() },
                        onDelete = { deletePlaylistId.value = item.id.toString() },
                    )


                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .border(
                                1.dp,
                                color =
                                    if (highlitedPlaylist1.value == item.name) highlitedColor else Color(0, 0, 0, 0)

                            )
                            .dashedBorder(
                                1.dp,
                                color = if (playlistDropDownMenuOpened.openedIndexName == item.id.toString())
                                    primary.copy(alpha = 0.5f)
                                else Color(0, 0, 0, 0)
                            )
                            .background(
                                if (highlitedPlaylist1.value == item.name)
                                    highlitedColor.copy(alpha = highlitedColor.alpha / 2)
                                else
                                    Color(0, 0, 0, 0)
                            )

                            .clickable {
                                openedAudioSource.value = item.playlistKey
                                AppPrefs.setString("openedAudioSource", item.playlistKey)
                            }
                    ) {

                        val sizeAnimated = animateFloatAsState(
                            targetValue = if (openedAudioSource.value != item.playlistKey) 1f else 1.6f
                        )

                        Box(
                            modifier = Modifier
                                .size(100.dp * sizeAnimated.value)
                                .aspectRatio(1f)
                                .background(Color(45, 45, 45))

                        ) {

                            if (openedAudioSource.value == item.playlistKey)
                            {
                                Box(Modifier.fillMaxSize().background(Color(0, 0, 0, 150)).border(BorderStroke(
                                    1.dp, Color(80, 80, 80)
                                )))
                                {
                                    Icon(Icons.Sharp.LastPage, "", modifier = Modifier.align(Alignment.Center).size(100.dp), tint = Color(255, 255, 255))
                                }
                            }

                        }

                        Column(
                            modifier = Modifier
                                .padding(start = 24.dp, end = 24.dp)
                                .fillMaxWidth()
                        ) {

                            Box() {

                                Row(verticalAlignment = Alignment.CenterVertically) {

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color.White
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = "playlist",
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }

                                    Box {
                                        IconButton(
                                            onClick = {

                                                playlistDropDownMenuOpened.openDropDown(
                                                    openIndexName = item.id.toString(),
                                                    openModeForSource = openMode.BUTTON_OPENED
                                                )

                                            },

                                        ){
                                            Icon(Icons.Sharp.MoreVert, "", tint = Color(255, 255, 255, 150))
                                        }


                                        createDropDownPlaylist(
                                            offsetFromIntToDp = DpOffset(0.dp, 0.dp),
                                            expanded =
                                                playlistDropDownMenuOpened.openedIndexName == item.id.toString() &&
                                                        playlistDropDownMenuOpened.open == openMode.BUTTON_OPENED
                                            ,
                                            onDismissRequest = { playlistDropDownMenuOpened.closeDropDown() },
                                            onRename = { renamePlaylistId.value = item.id.toString() },
                                            onDelete = { deletePlaylistId.value = item.id.toString() },
                                        )



                                    }

                                }

                            }



                        }
                    }
                }

                HorizontalDivider(Modifier.fillMaxWidth()
                    .padding(vertical = 0.dp, horizontal = 64.dp), thickness = 1.dp,
                    color = Color(255, 255, 255, 10))

            }
        }

        ScrollProgressThumb(
            scrollFraction = scrollFraction,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 0.dp) // левее букв
        )




    }
}

@OptIn(FlowPreview::class, ExperimentalComposeUiApi::class)
@Composable
fun playlistTab(
    listState: LazyListState,
    audioFolderController: AudioFolderController,
    openedAudioSource: MutableState<String>,
    playlistController: PlaylistController,
) {

    val primary = MaterialTheme.colorScheme.primary

    var searchQr by rememberSaveable { mutableStateOf("") }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var isFocused by rememberSaveable { mutableStateOf(false) }
    var queryChangedByUser by rememberSaveable { mutableStateOf(false) }

    var playlistCreateWindow by remember { mutableStateOf(false) }
    var deletePlaylistId = remember { mutableStateOf("") }
    var renamePlaylistId = remember { mutableStateOf("") }
    var renamePlaylistString = remember { mutableStateOf("") }
    var highlitedPlaylist = remember { mutableStateOf("") }

    val audioMap by playlistController.playlists.collectAsState()

    Column(Modifier.padding(horizontal = 32.dp).fillMaxSize()) {

        val painter = rememberVectorPainter(Icons.Sharp.PlaylistAddCheckCircle)

        wizui.wizBlinkingText(
            "playlists",
            normalColor = Color(255, 255, 255),
            blinkColor = MaterialTheme.colorScheme.primary,
            fontSize = 32.sp,
            modifier = Modifier.padding(start = 12.dp),
            onClick = {

            }
        )

        Box(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth()
                .drawBehind {

                    val iconSize = 210.dp.toPx()
                    val iconOffsetX = size.width - 40.dp.toPx() - iconSize
                    val centerY = size.height / 2

                    clipRect(
                        left = iconOffsetX,
                        top = centerY - iconSize / 2,
                        right = iconOffsetX + iconSize,
                        bottom = centerY
                    ) {
                        translate(
                            left = iconOffsetX,
                            top = centerY - iconSize / 2
                        ) {
                            // 🔑 ВАЖНО: painter — receiver
                            with(painter) {
                                draw(
                                    size = Size(iconSize, iconSize),
                                    colorFilter = ColorFilter.tint(
                                        Color(255, 255, 255, 20)
                                    )
                                )
                            }
                        }
                    }
                }
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val albums by remember {
            derivedStateOf {
                audioMap
            }
        }

        // Дебаунсинг — без изменений, но интегрируем с derivedStateOf ниже
        LaunchedEffect(Unit) {
            snapshotFlow { searchQr }
                .debounce(300)
                .collect { value ->
                    debouncedQuery = value
                }
        }

        // Оптимизация: derivedStateOf вместо produceState — ленивее, без корутин в UI
        val results = remember { mutableStateListOf<Playlist>() }

        LaunchedEffect(albums, debouncedQuery) {
            results.clear()
            results.addAll(
                if (debouncedQuery.isBlank()) albums.reversed()
                else albums.reversed().filter { matchesQueryPlaylist(debouncedQuery, it) }
            )
        }

        Spacer(Modifier.height(8.dp))

        wizui.wizButton(
            backgroundColor = Color(0, 0, 0, 0),
            modifier = Modifier.fillMaxWidth().height(60.dp).border(
                BorderStroke(0.5.dp, Color(255, 255, 255, 100))),
            contentColor = Color.White,
            shape = RectangleShape,
            onClick = {
                playlistCreateWindow = true
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Sharp.PlaylistAdd, "", tint = Color.White)
                Text("create playlist", modifier = Modifier.padding(horizontal = 12.dp))
            }
        }

        Spacer(Modifier.height(16.dp))


        Box {

            if (results.isNotEmpty()) {


                Box(Modifier.onSizeChanged { size ->

                }) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .align(Alignment.TopCenter)
                            .zIndex(1f)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(20, 20, 20), Color.Black.copy(alpha = 0f))
                                )
                            )
                    )

                    playlistsWithAlphabetScroller(
                        results = results,
                        listState = listState,
                        highlitedPlaylist,
                        openedAudioSource = openedAudioSource,
                        deletePlaylistId,
                        renamePlaylistId

                    )

                }
            }

            // Поиск-бар — без больших изменений, но hazeEffect отложен
            Row(Modifier.padding(top = 8.dp).zIndex(3f)) {
                BasicTextField(
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    value = searchQr,
                    onValueChange = {
                        searchQr = it
                        queryChangedByUser = true
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .weight(1f)
                        .height(40.dp)
                        .background(Color(25, 25, 25, 150))
                        .drawBehind {

                            val y = size.height

                            drawLine(
                                if (isFocused) primary  else Color(255, 255, 255, 100),
                                Offset(0f, y),
                                Offset(size.width, y),
                                1f
                            )
                        }
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQr.isEmpty() && !isFocused) {

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Sharp.Search, "", tint =
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        "searching",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                            innerTextField()
                        }
                    }
                )

            }
        }

        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Sharp.Folder, "",
                    tint = Color(255, 255, 255, 30),
                    modifier = Modifier.size(150.dp)
                )
                Text("nothing here :)", color = Color.White)
            }
        }
    }


    handlePlaylistPopUps(

        playlistCreateWindow            = playlistCreateWindow,
        onPlaylistCreateWindowChange    = { playlistCreateWindow = it },

        newPlaylistName         = newPlaylistName,
        onNewPlaylistNameChange = { newPlaylistName = it },

        deletePlaylistId        = deletePlaylistId,
        renamePlaylistId        = renamePlaylistId,
        renamePlaylistString    = renamePlaylistString,
        highlitedPlaylist       = highlitedPlaylist,

        audioMap                = audioMap,
        playlistController      = playlistController,

        primary                 = primary
    )





}
