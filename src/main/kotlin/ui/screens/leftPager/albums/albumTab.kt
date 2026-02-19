package ui.screens.leftPager.albums

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.v2.maxScrollOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Folder
import androidx.compose.material.icons.sharp.LastPage
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.audioindex.AudioFolderController
import org.example.audioindex.ScannedAudio
import org.example.similarity
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.wizui.wizui
import ui.uiHelpers.relativeLetterSpacing
import kotlin.math.roundToInt
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import kotlinx.coroutines.flow.filterNotNull
import org.example.ui.uiHelpers.wizuiUIMove

private fun albumKey(a: ScannedAudio): String =
    "${a.album}::${a.year}"

fun buildAlbumRepresentatives(
    audioMap: Map<String, ScannedAudio>
): List<ScannedAudio> =
    audioMap.values
        .groupBy { albumKey(it) }
        .map { (_, tracks) ->
            tracks.firstOrNull { it.artworkPath != null }
                ?: tracks.first()
        }

fun matchesQuery(query: String, album: ScannedAudio): Boolean {
    if (query.isBlank()) return true

    val words = query
        .lowercase()
        .split(" ")
        .filter { it.isNotBlank() }

    val fields = listOf(
        album.album,
        album.artist,
        album.year
    ).map { it.lowercase() }

    return words.all { word ->
        fields.any { field -> field.contains(word) }
    }
}

fun albumScore(query: String, album: ScannedAudio): Float {
    val q = query.lowercase()
    return maxOf(
        similarity(q, album.album.lowercase()),
        similarity(q, album.artist.lowercase())
    )
}

@Composable
fun albumsWithAlphabetScroller(
    results: List<ScannedAudio>,
    listState: LazyListState,
    openedAudioSource: MutableState<String>
) {
    val scope = rememberCoroutineScope()
    val scrollFraction = rememberScrollFraction(listState)

    // ───── Alphabet ─────
    val letters = remember(results) {
        results
            .mapNotNull { it.album.firstOrNull()?.uppercaseChar() }
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
                        it.album.firstOrNull()?.uppercaseChar() == letter
                    }
                )
            }
        }
    }

    var bubbleLetter by remember { mutableStateOf<Char?>(null) }
    var alphabetHeightPx by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {

        
        // ───── YOUR LazyColumn ─────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 69.dp, bottom = 16.dp)
        ) {
            items(
                items = results,
                key = { it.albumKey },
                contentType = { "album" }
            ) { item ->

                HorizontalDivider(Modifier.fillMaxWidth()
                    .padding(vertical = 0.dp, horizontal = 64.dp), thickness = 1.dp,
                    color = Color(255, 255, 255, 10))

                Box {


                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                openedAudioSource.value = item.albumKey
                                AppPrefs.setString("openedAudioSource", item.albumKey)
                            }
                    ) {

                        val sizeAnimated = animateFloatAsState(
                            targetValue = if (openedAudioSource.value != item.albumKey) 1f else 1.6f
                        )

                        Box(
                            modifier = Modifier
                                .size(100.dp * sizeAnimated.value)
                                .aspectRatio(1f)
                                .background(Color(45, 45, 45))

                        ) {
                            artworkAsync(
                                item.artworkPath,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (openedAudioSource.value == item.albumKey)
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
                                text = item.album,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = item.artist,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White.copy(alpha = 0.5f)
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = item.year,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White.copy(alpha = 0.3f)
                            )
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

@Composable
fun rememberScrollFraction(listState: LazyListState): Float {

    val adapter = rememberScrollbarAdapter(listState)

    val fraction by remember {
        derivedStateOf {

            val max = adapter.maxScrollOffset

            if (max <= 0.0) 0f
            else (adapter.scrollOffset / max).toFloat()
        }
    }

    return fraction.coerceIn(0f, 1f)
}

@Composable
fun ScrollProgressThumb(
    scrollFraction: Float,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 36.dp
) {
    var containerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    Box(
        modifier = modifier
            .padding(top = 76.dp, bottom = 28.dp)
            .width(1.dp)
            .fillMaxHeight()
            .onSizeChanged {
                containerHeightPx = it.height
            }
    ) {
        // ───── Track ─────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.White.copy(alpha = 0.10f),
                )
        )

        if (containerHeightPx > 0) {
            val maxOffset =
                (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)

            val thumbOffsetY =
                maxOffset * scrollFraction.coerceIn(0f, 1f)

            // ───── Thumb ─────
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffsetY.toInt()) }
                    .width(3.dp)
                    .height(thumbHeight)
                    .background(
                        MaterialTheme.colorScheme.primary,
                    )
            )
        }
    }
}

@Composable
fun AlphabetBubble(letter: Char) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter.toString(),
                fontSize = 48.sp,
                color = Color.White
            )
        }
    }
}

fun handleAlphabetTouch(
    y: Float,
    heightPx: Int,
    letters: List<Char>,
    letterToIndex: Map<Char, Int>,
    scope: CoroutineScope,
    listState: LazyListState,
    onLetterChanged: (Char) -> Unit
) {
    if (heightPx == 0) return

    val letterHeight = heightPx / letters.size
    val index = (y / letterHeight)
        .toInt()
        .coerceIn(0, letters.lastIndex)

    val letter = letters[index]
    onLetterChanged(letter)

    val targetIndex = letterToIndex[letter] ?: return
    scope.launch {
        listState.scrollToItem(targetIndex)
    }
}


@OptIn(FlowPreview::class, ExperimentalComposeUiApi::class)
@Composable
fun albumTab(
    listState: LazyListState,
    audioFolderController: AudioFolderController,
    openedTab: MutableState<Int>,
    gridMultiplier: MutableState<Float>,
    openedAudioSource: MutableState<String>,
) {

    val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

    var searchQr by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var isFocused by rememberSaveable { mutableStateOf(false) }
    var queryChangedByUser by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 32.dp).fillMaxSize()) {

        val painter = rememberVectorPainter(Icons.Sharp.Album)

        wizui.wizBlinkingText(
            "albums",
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

        val audioMap by audioFolderController.audioMap.collectAsState()

        val albums by remember {
            derivedStateOf {
                buildAlbumRepresentatives(audioMap)
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
                        .filter { matchesQuery(debouncedQuery, it) }
                        .sortedByDescending { albumScore(debouncedQuery, it) }
                }
        }


        // Оптимизация: animateScrollToItem для плавности, без stopScroll (оно может джанкать)
        LaunchedEffect(debouncedQuery, queryChangedByUser) {
            if (!queryChangedByUser) return@LaunchedEffect
            listState.animateScrollToItem(0)
            gridState.animateScrollToItem(0)
            queryChangedByUser = false
        }

        Box {

            if (results.isNotEmpty()) {

                val baseCardWidth = 160.dp
                val baseTitleFont = 14.sp
                val baseArtistFont = 10.sp

                var gridWidth by remember { mutableStateOf(0.dp) }  // Предполагаю, ты где-то обновляешь; если нет, используй onSizeChanged
                val itemWidth by remember { mutableStateOf(0.dp) }  // Аналогично

                val scale by remember(gridMultiplier.value, itemWidth) {
                    derivedStateOf {
                        if (gridMultiplier.value.roundToInt() == 0) {
                            val adaptiveColumns = maxOf(1, (gridWidth / baseCardWidth).toInt())
                            lerp(
                                start = 1.5f,
                                stop = 0.6f,
                                fraction = ((adaptiveColumns - 1) / 6f).coerceIn(0f, 1f)
                            )
                        } else {
                            (itemWidth / baseCardWidth).coerceIn(0.2f, 1.5f)
                        }
                    }
                }

                val titleFontSize by remember(scale) { mutableStateOf(baseTitleFont * scale) }
                val artistFontSize by remember(scale) { mutableStateOf(baseArtistFont * scale) }

                val density = LocalDensity.current

                Box(Modifier.onSizeChanged { size ->
                    gridWidth = with(density) { size.width.toDp() }
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

                    if (drawGrid)
                    {
                        LazyVerticalGrid(
                            columns = if (gridMultiplier.value.roundToInt() != 0)
                                GridCells.Fixed(gridMultiplier.value.roundToInt())
                            else
                                GridCells.Adaptive(160.dp),
                            modifier = Modifier.padding(),
                            state = gridState,
                            userScrollEnabled = true,
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 69.dp, bottom = 16.dp),

                            ) {
                            itemsIndexed(
                                items = results,
                                contentType = { _, _ -> "album" },
                                key = { _, album -> album.albumKey }
                            ) { index, item ->


                                Column(
                                    modifier = Modifier.clickable {
                                        openedAudioSource.value = item.albumKey
                                        AppPrefs.setString("openedAudioSource", item.albumKey)
                                    }
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(Color(45, 45, 45))
                                    ) {
                                        artworkAsync(
                                            item.artworkPath,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    if (titleFontSize > 9.5.sp) {
                                        Column(modifier = Modifier.padding(top = 9.dp * scale)) {
                                            Text(
                                                text = item.album,
                                                fontSize = titleFontSize,
                                                letterSpacing = relativeLetterSpacing(titleFontSize),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White
                                            )

                                            Spacer(Modifier.height(4.dp * scale))

                                            Text(
                                                text = item.artist,
                                                fontSize = artistFontSize,
                                                letterSpacing = relativeLetterSpacing(artistFontSize),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else
                    {

                        LaunchedEffect(Unit)
                        {
                            snapshotFlow { wizuiUIMove.albumListMoveToAlbumKey }
                                .filterNotNull()
                                .collect { key ->

                                    val index = results.indexOfFirst { it.albumKey == key }

                                    if (index != -1)
                                        listState.animateScrollToItem(index)

                                    wizuiUIMove.albumListMoveToAlbumKey = null
                                }
                        }

                        albumsWithAlphabetScroller(
                            results = results,
                            listState = listState,
                            openedAudioSource = openedAudioSource
                        )


                    }

                }
            }

            val primary = MaterialTheme.colorScheme.primary
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
}
