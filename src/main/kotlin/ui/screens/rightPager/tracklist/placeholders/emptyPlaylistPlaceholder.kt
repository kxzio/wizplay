package org.example.ui.screens.rightPager.tracklist.placeholders

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Queue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.bass.bassController.trackSource
import org.example.folderGetter.PlaylistController
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.wizui.wizui
import kotlin.io.path.Path

@Composable
fun emptyPlaylistPlaceHolder(
    openedAudioSource: MutableState<String>,
    playlistController: PlaylistController
)
{
    Box(Modifier.fillMaxSize().padding(bottom = 142.dp))
    {
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