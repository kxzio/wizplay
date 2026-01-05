package ui.screens.rightPager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PermMedia
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.audioindex.AudioFolderController
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.wizui.wizui
import ui.uiHelpers.relativeLetterSpacing

@Composable
fun renderRightPager(audioFolderController: AudioFolderController, openedAudioSource: MutableState<String>)
{

    wizui.wizColumn(modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth()
        .background(Color(16, 16, 16))
        .padding(16.dp)
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                Icon(Icons.Sharp.PermMedia, "",
                    tint = Color(255, 255, 255, 30),
                    modifier = Modifier.size(150.dp)
                )

                Text("select album or playlist from the media-tab", fontSize = 16.sp, color = Color(255, 255, 255))
            }
        else
        {
            var trackWithArtOrFirst = openedAlbumTracks.firstOrNull { it.artworkPath != null }
            if (trackWithArtOrFirst == null)
                trackWithArtOrFirst = openedAlbumTracks.first()

            LazyColumn {

                item {

                    Column(modifier = Modifier.padding(start = 16.dp))
                    {
                        Spacer(Modifier.height(8.dp))

                        Box(Modifier.size(350.dp)) {
                            artworkAsync(
                                trackWithArtOrFirst.artworkPath,
                                Modifier.size(350.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            trackWithArtOrFirst.album,
                            fontSize = 26.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(255, 255, 255)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            trackWithArtOrFirst.artist,
                            fontSize = 18.sp,
                            color = Color(255, 255, 255, 120)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            if (trackWithArtOrFirst.year.isEmpty()) "Year not specified" else trackWithArtOrFirst.year,
                            fontSize = 18.sp,
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
                        onClick = { }
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
                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(start = 16.dp).fillMaxWidth()) {
                                Text(item.title, fontSize = 16.sp,)
                                Spacer(Modifier.height(4.dp))
                                Text(item.artist, fontSize = 12.sp, color = Color(255, 255, 255, 100))
                            }
                        }

                    }
                }
            }



        }

    }
}