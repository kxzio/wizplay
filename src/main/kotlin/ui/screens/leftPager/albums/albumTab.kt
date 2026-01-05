package ui.screens.leftPager.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import org.example.audioindex.AudioFolderController
import org.example.audioindex.ScannedAudio
import org.example.similarity
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf
import ui.uiHelpers.relativeLetterSpacing
import java.nio.file.Path

private fun albumKey(a: ScannedAudio): String =
    "${a.artist}::${a.album}::${a.year}"

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

    val q = query.lowercase()

    return listOf(
        album.album,
        album.artist,
        album.year
    ).any { field ->
        field.lowercase().contains(q)
    }
}

fun albumScore(query: String, album: ScannedAudio): Float {
    if (query.isBlank()) return 1f

    val q = query.lowercase()

    val albumScore  = similarity(q, album.album)
    val artistScore = similarity(q, album.artist)

    return albumScore * 0.7f + artistScore * 0.3f
}

@OptIn(FlowPreview::class)
@Composable
fun albumTab(
    audioFolderController: AudioFolderController,
    openedTab: MutableState<Int>,
    gridMultiplier: MutableState<Float>,
    openedAudioSource: MutableState<String>,
)
{
    var searchQr  by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    (openedTab.value == 2).wizAnimateIf(wizui.WizAnimationType.ExpandVertically) {
        Column(Modifier.padding(top = 8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                wizui.wizBlinkingText(
                    "albums",
                    normalColor = Color(255, 255, 255),
                    blinkColor = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 12.dp),
                    onClick = {

                    }
                )

            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                thickness = 1.0.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Row {
                BasicTextField(
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    value = searchQr,
                    onValueChange = { searchQr = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
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

                        (searchQr.isEmpty() && !isFocused).wizAnimateIf {
                            Text(
                                "searching",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        innerTextField()
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

            if (!results.isEmpty())
            {
                wizui.wizVerticalGrid(
                    dynamicColumnsCount = true,
                    modifier = Modifier.padding(top = 16.dp),
                    userScrollEnabled = true,
                    dynamicMinSizeForElement = 160.dp * gridMultiplier.value,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 16.dp,
                        alignment = Alignment.Start
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    items = results,
                )
                { item ->

                    Column(Modifier.clickable {
                        openedAudioSource.value = item.albumKey
                        AppPrefs.setString("openedAudioSource", item.albumKey)
                    })
                    {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(45, 45, 45))) {

                            artworkAsync(
                                item.artworkPath,
                                Modifier.fillMaxSize()
                            )

                        }

                        Column(modifier = Modifier.padding(top = 9.dp * gridMultiplier.value)) {

                            Text(item.album,
                                fontSize = 16.sp * gridMultiplier.value,
                                letterSpacing = relativeLetterSpacing(16.sp * gridMultiplier.value),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(255, 255, 255))

                            Spacer(Modifier.height(4.dp * gridMultiplier.value))

                            Text(item.artist,
                                letterSpacing = relativeLetterSpacing(11.sp * gridMultiplier.value),
                                fontSize = 11.sp * gridMultiplier.value,
                                color = Color(255, 255, 255, 100))
                        }

                    }

                }
            }
            else
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