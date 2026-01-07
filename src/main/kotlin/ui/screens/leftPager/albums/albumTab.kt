package ui.screens.leftPager.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.example.audioindex.AudioFolderController
import org.example.audioindex.ScannedAudio
import org.example.similarity
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf
import ui.uiHelpers.relativeLetterSpacing
import java.nio.file.Path
import kotlin.math.roundToInt

private fun albumKey(a: ScannedAudio): String =
    "${a.album}::${a.year}"

fun buildAlbumRepresentatives(
    audioMap: Map<Path, ScannedAudio>
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
    if (query.isBlank()) return 1f

    val q = query.lowercase()

    val albumScore  = similarity(q, album.album)
    val artistScore = similarity(q, album.artist)

    return albumScore * 0.7f + artistScore * 0.3f
}

@OptIn(FlowPreview::class, ExperimentalComposeUiApi::class)
@Composable
fun albumTab(
    audioFolderController: AudioFolderController,
    openedTab: MutableState<Int>,
    gridMultiplier: MutableState<Float>,
    openedAudioSource: MutableState<String>,
)
{
    val gridState = rememberLazyGridState()
    var searchQr  by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    (openedTab.value == 2).wizAnimateIf(wizui.WizAnimationType.ExpandVertically) {
        Column(Modifier.padding(horizontal = 32.dp)) {

            val albums = remember(audioFolderController.audioMap.value) {
                buildAlbumRepresentatives(audioFolderController.audioMap.value)
            }

            LaunchedEffect(Unit) {
                snapshotFlow { searchQr }
                    .debounce(300)
                    .collect { value ->
                        debouncedQuery = value
                    }
            }

            val results = remember(debouncedQuery, albums) {
                albums
                    .filter { matchesQuery(searchQr, it) }
                    .map { album ->
                        album to albumScore(searchQr, album)
                    }
                    .sortedByDescending { it.second }
                    .map { it.first }

            }

            LaunchedEffect(results) {
                gridState.stopScroll()
                gridState.scrollToItem(0)
            }

            Box {

                val hazeState = rememberHazeState()

                if (!results.isEmpty())
                {

                    val BaseCardWidth = 160.dp
                    val BaseTitleFont = 14.sp
                    val BaseArtistFont = 10.sp

                    var gridWidth by remember { mutableStateOf(0.dp) }

                    var itemWidth by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current

                    val scale by remember {
                        derivedStateOf {
                            if (gridMultiplier.value.roundToInt() == 0) {
                                val adaptiveColumns =
                                    maxOf(1, (gridWidth / BaseCardWidth).toInt())

                                lerp(
                                    start = 1.5f,
                                    stop = 0.6f,
                                    fraction = ((adaptiveColumns - 1) / 6f).coerceIn(0f, 1f)
                                )
                            } else {
                                (itemWidth / BaseCardWidth)
                                    .coerceIn(0.2f, 1.5f)
                            }
                        }
                    }

                    val titleFontSize = BaseTitleFont * scale
                    val artistFontSize = BaseArtistFont * scale


                    Box(Modifier.hazeSource(hazeState)) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .align(Alignment.TopCenter)
                                .zIndex(1f)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(20, 20, 20),
                                            Color.Black.copy(alpha = 0f)
                                        )
                                    )
                                )
                        )

                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize().onSizeChanged {
                                gridWidth = with(density) { it.width.toDp() }
                            }
                        ) {
                            val baseSpacing = 16.dp
                            val spacing =
                                if (gridMultiplier.value.roundToInt() == 0)
                                    baseSpacing
                                else
                                    baseSpacing * scale

                            val columns =
                                if (gridMultiplier.value.roundToInt() != 0)
                                    gridMultiplier.value.roundToInt()
                                else
                                    maxOf(1, (maxWidth / 160.dp).toInt())

                            val totalSpacing = spacing * (columns - 1)
                            val cellWidth = (maxWidth - totalSpacing) / columns

                            // ⬇️ ВОТ ТВОЙ ИДЕАЛЬНЫЙ WIDTH
                            LaunchedEffect(cellWidth) {
                                itemWidth = cellWidth
                            }

                            val gridSpacing =
                                if (gridMultiplier.value.roundToInt() == 0)
                                    16.dp
                                else
                                    16.dp * scale

                            LazyVerticalGrid(
                                columns =
                                    if (gridMultiplier.value.roundToInt() != 0)
                                        GridCells.Fixed(gridMultiplier.value.roundToInt())
                                    else
                                        GridCells.Adaptive(160.dp),

                                modifier = Modifier.padding(),
                                state = gridState,
                                userScrollEnabled = true,

                                horizontalArrangement = Arrangement.spacedBy(
                                    space = gridSpacing,
                                    alignment = Alignment.Start
                                ),
                                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                                contentPadding = PaddingValues(
                                    top = 69.dp,
                                    bottom = 16.dp
                                ),

                            ) {


                                itemsIndexed(
                                    items = results,
                                    key = { _, album -> album.albumKey }
                                ) { index, item ->

                                    Box(
                                        modifier = Modifier.animateItem()
                                    ) {

                                        Column(
                                            modifier = Modifier
                                                .clickable {
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
                                                    Modifier.fillMaxSize()
                                                )
                                            }

                                            if (titleFontSize > 9.5.sp) {
                                                Column(
                                                    modifier = Modifier.padding(top = 9.dp * scale)
                                                ) {

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
                        }

                    }
                }

                Row(Modifier.padding(top = 8.dp).zIndex(3f)) {
                    BasicTextField(
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        value = searchQr,
                        onValueChange = { searchQr = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier

                            .padding(bottom = 16.dp)
                            .weight(1f)
                            .height(40.dp)
                            .hazeEffect(
                                hazeState,
                                style = HazeStyle(
                                    backgroundColor = Color(15, 15, 15),
                                    blurRadius = 25.dp,
                                    tint = (HazeTint(
                                        color = Color(0, 0, 0, 0)
                                    )),
                                    noiseFactor = 0.15f
                                )
                            )
                            .background(Color(25, 25, 25, 150))
                            .border(
                                width = 0.5.dp,
                                color = if (searchQr.isNotEmpty() || isFocused)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = RectangleShape
                            )
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart // или Center, CenterEnd
                            ) {
                                if (searchQr.isEmpty() && !isFocused) {
                                    Text(
                                        "searching",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                innerTextField()
                            }
                        }

                    )

                    (!searchQr.isEmpty()).wizAnimateIf(wizui.WizAnimationType.ExpandHorizontally) {
                        Button({
                            searchQr = ""
                        },
                            modifier = Modifier.height(40.dp),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                contentColor = Color(255, 255, 255),
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        {
                            Text("clear")
                        }
                    }

                }
            }




            if (results.isEmpty())
            {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
                {

                    Icon(Icons.Sharp.Folder, "",
                        tint = Color(255, 255, 255, 30),
                        modifier = Modifier.size(150.dp)
                    )

                    Text("nothing here :)", color = Color(255, 255, 255))

                }
            }

        }
    }

}