package org.example.ui.screens.leftPager.playlists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.PlaylistAddCheckCircle
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toString
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.OS
import org.example.audioindex.AudioFolderController
import org.example.audioindex.ScannedAudio
import org.example.folderGetter.Playlist
import org.example.folderGetter.PlaylistController
import org.example.pickFolderLinuxNative
import org.example.pickFolderWindowsNative
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.ui.uiHelpers.AniJinPopup
import org.example.ui.uiHelpers.wizuiUIMove
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf
import ui.screens.leftPager.albums.AlphabetBubble
import ui.screens.leftPager.albums.ScrollProgressThumb
import ui.screens.leftPager.albums.albumScore
import ui.screens.leftPager.albums.albumsWithAlphabetScroller
import ui.screens.leftPager.albums.buildAlbumRepresentatives
import ui.screens.leftPager.albums.handleAlphabetTouch
import ui.screens.leftPager.albums.matchesQuery
import ui.screens.leftPager.albums.rememberScrollFraction
import kotlin.collections.component1
import kotlin.collections.component2

fun matchesQueryPlaylist(query: String, playlist: Playlist): Boolean {
    if (query.isBlank()) return true

    val words = query
        .lowercase()
        .split(" ")
        .filter { it.isNotBlank() }

    val fields = listOf(
        playlist.name
    ).map { it.lowercase() }

    return words.all { word ->
        fields.any { field -> field.contains(word) }
    }
}

@Composable
fun playlistsWithAlphabetScroller(
    results: List<Playlist>,
    listState: LazyListState,
    openedAudioSource: MutableState<String>
) {
    val scope = rememberCoroutineScope()
    val scrollFraction = rememberScrollFraction(listState)

    // ───── Alphabet ─────
    val letters = remember(results) {
        results
            .mapNotNull { it.name.firstOrNull()?.uppercaseChar() }
            .distinct()
            .sorted()
    }

    // ───── Letter → index ─────
    val letterToIndex = remember(results) {
        buildMap {
            letters.forEach { letter ->
                put(
                    letter,
                    results.indexOfFirst {
                        it.name.firstOrNull()?.uppercaseChar() == letter
                    }
                )
            }
        }
    }

    var bubbleLetter by remember { mutableStateOf<Char?>(null) }
    var alphabetHeightPx by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {

        val isOpenedAlbumVisible = listState.layoutInfo.visibleItemsInfo.map { it.key }.contains(openedAudioSource.value)

        // ───── YOUR LazyColumn ─────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 69.dp, bottom = 16.dp)
        ) {
            items(
                items = results,
                key = { it.id },
                contentType = { "playlist" }
            ) { item ->

                HorizontalDivider(Modifier.fillMaxWidth()
                    .padding(vertical = 0.dp, horizontal = 64.dp), thickness = 1.dp,
                    color = Color(255, 255, 255, 10))

                Box {


                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                openedAudioSource.value = item.id.toString()
                                AppPrefs.setString("openedAudioSource", item.id.toString())
                            }
                    ) {

                        val sizeAnimated = animateFloatAsState(
                            targetValue = if (openedAudioSource.value != item.id.toString()) 1f else 1.6f
                        )

                        Box(
                            modifier = Modifier
                                .size(100.dp * sizeAnimated.value)
                                .aspectRatio(1f)
                                .background(Color(45, 45, 45))

                        ) {

                            if (openedAudioSource.value == item.id.toString())
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
                    }
                }

                HorizontalDivider(Modifier.fillMaxWidth()
                    .padding(vertical = 0.dp, horizontal = 64.dp), thickness = 1.dp,
                    color = Color(255, 255, 255, 10))

            }
        }

        val cour = rememberCoroutineScope()

        AnimatedVisibility(visible = !isOpenedAlbumVisible, Modifier.align(Alignment.BottomStart)) {

            val interactionSource = remember { MutableInteractionSource() }
            OutlinedButton({
                cour.launch {
                    listState.animateScrollToItem(results.indexOfFirst { it.id.toString() == openedAudioSource.value})
                }
            }, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).animateContentSize(

            ),
                elevation = ButtonDefaults.elevatedButtonElevation(),
                interactionSource = interactionSource,
                border = BorderStroke(0.75.dp, Color(255, 255, 255, 100)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(20, 20, 20)
                )
            )
            {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(Icons.Sharp.LastPage, "",
                        tint = Color(255, 255, 255),
                        modifier = Modifier.size(40.dp)
                    )

                    if (interactionSource.collectIsHoveredAsState().value) {
                        Text("go to opened!",
                            color = Color(255, 255, 255),
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }



                }

            }
        }



        ScrollProgressThumb(
            scrollFraction = scrollFraction,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 0.dp) // левее букв
        )

        // ───── Alphabet bar ─────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 76.dp, bottom = 16.dp)
                .fillMaxHeight()
                .width(24.dp)
                .onSizeChanged { alphabetHeightPx = it.height }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            handleAlphabetTouch(
                                offset.y,
                                alphabetHeightPx,
                                letters,
                                letterToIndex,
                                scope,
                                listState
                            ) { bubbleLetter = it }
                        },
                        onVerticalDrag = { change, _ ->
                            handleAlphabetTouch(
                                change.position.y,
                                alphabetHeightPx,
                                letters,
                                letterToIndex,
                                scope,
                                listState
                            ) { bubbleLetter = it }
                        },
                        onDragEnd = {
                            bubbleLetter = null
                        }
                    )
                }
        ) {
            letters.forEach {
                Text(
                    text = it.toString(),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ───── Bubble ─────
        bubbleLetter?.let { letter ->
            AlphabetBubble(letter)
        }
    }
}

@OptIn(FlowPreview::class, ExperimentalComposeUiApi::class)
@Composable
fun playlistTab(
    audioFolderController: AudioFolderController,
    openedAudioSource: MutableState<String>,
    playlistController: PlaylistController,
) {

    val primary = MaterialTheme.colorScheme.primary

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    var searchQr by rememberSaveable { mutableStateOf("") }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var isFocused by rememberSaveable { mutableStateOf(false) }
    var queryChangedByUser by rememberSaveable { mutableStateOf(false) }
    var playlistCreateWindow by remember { mutableStateOf(false) }

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

        val audioMap by playlistController.playlists.collectAsState()

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
        val results by produceState(
            initialValue = albums,
            albums,
            debouncedQuery
        ) {
            value =
                if (debouncedQuery.isBlank()) albums
                else withContext(Dispatchers.Default) {
                    albums
                        .filter { matchesQueryPlaylist(debouncedQuery, it) }
                }
        }

        LaunchedEffect(wizuiUIMove.albumListMoveToAlbumKey)
        {
            val index = results.indexOfFirst { it.id.toString() == openedAudioSource.value}
            if (index != -1) listState.animateScrollToItem(index)
            wizuiUIMove.albumListMoveToAlbumKey = ""
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

                    val drawGrid = false

                    playlistsWithAlphabetScroller(
                        results = results,
                        listState = listState,
                        openedAudioSource = openedAudioSource
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


    AniJinPopup(
        focusable = true,
        expanded = playlistCreateWindow,
        onDismissRequest = { playlistCreateWindow = false },
        enter =
            fadeIn(
                animationSpec = tween(120)
            ) +
            scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        exit =
            fadeOut(
                animationSpec = tween(90)
            ) +
            scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(120)
            ),
        content = {
            Box(Modifier.fillMaxSize().background(Color(0, 0, 0, 100))) {
                Surface(
                    color = Color(20, 20, 20),
                    border = BorderStroke(0.5.dp, Color(255, 255, 255, 100)),
                    modifier = Modifier.padding(16.dp).align(Alignment.Center)
                ) {

                    Column(Modifier.padding(32.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            IconButton( {
                                playlistCreateWindow = false
                            }){
                                Icon(Icons.Sharp.Close, "", tint = Color.White)
                            }

                            Text("create new playlist", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }


                        Spacer(Modifier.height(8.dp))
                        var isFocusedOnCreation by rememberSaveable { mutableStateOf(false) }
                        Row(Modifier.padding(top = 8.dp).zIndex(3f)) {
                            BasicTextField(
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                value = newPlaylistName,
                                onValueChange = {
                                    newPlaylistName = it
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .width(400.dp)
                                    .height(40.dp)
                                    .background(Color(25, 25, 25, 150))
                                    .drawBehind {

                                        val y = size.height

                                        drawLine(
                                            if (isFocusedOnCreation) primary else Color(255, 255, 255, 100),
                                            Offset(0f, y),
                                            Offset(size.width, y),
                                            1f
                                        )
                                    }
                                    .onFocusChanged { focusState ->
                                        isFocusedOnCreation = focusState.isFocused
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (newPlaylistName.isEmpty() && !isFocusedOnCreation) {

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Sharp.Create, "", tint =
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                )
                                                Text(
                                                    "playlist name",
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
                }
            }
        }
    )




}
