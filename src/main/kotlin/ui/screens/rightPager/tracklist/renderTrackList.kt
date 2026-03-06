package org.example.ui.screens.rightPager.tracklist

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.coreMaster.grooviqCore
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.example.audioindex.ScannedAudio
import org.example.bass.bassController.trackSource

import org.example.folderGetter.PlaylistController
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.ui.screens.leftPager.albums.drawAudioSourceArtwork
import org.example.ui.screens.leftPager.playlists.dropDownMenuOpenMode
import org.example.ui.screens.rightPager.tracklist.track.drawTrack
import ui.screens.rightPager.formatDuration
import ui.screens.rightPager.tracklist.track.dropdownAndPopups.handleTracksPopUp
import java.nio.file.Path

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun drawTrackList(
    openedAlbumTracks: List<ScannedAudio>,
    playlistController: PlaylistController,
    listState: LazyListState,
    hazeState: HazeState,
    openedAudioSource: MutableState<String>,
    offsetOfBottomBar: MutableState<Dp>,
)
{
    var trackWithArtOrFirst = openedAlbumTracks.firstOrNull { it.artworkPath != null }
    if (trackWithArtOrFirst == null)
        trackWithArtOrFirst = openedAlbumTracks.firstOrNull()

    val hasMultipleDiscs =
        openedAlbumTracks
            .map { it.disc }
            .distinct()
            .size > 1

    val trackDropDownOpen = remember { dropDownMenuOpenMode() }

    val albumDurationCache = remember { mutableMapOf<String, String>() }

    val currentAlbumDuration by produceState<String>(
        initialValue = "–",
        key1 = openedAudioSource.value
    ) {
        value = albumDurationCache.getOrPut(openedAudioSource.value) {
            formatDuration(
                grooviqCore.controllers.audioController.bassAudioController.getAlbumDurationSec(openedAlbumTracks)
            )
        }
    }

    var targetTrackPopup = remember { mutableStateOf<ScannedAudio?>(null) }

    val isAlbum = trackSource.fromString(openedAudioSource.value) is trackSource.album

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

                            if (isAlbum)
                            {
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
                            else {
                                drawAudioSourceArtwork(
                                    pathes = openedAlbumTracks.map { it.artworkPath } ,
                                    Modifier.size(250.dp)
                                )
                            }

                        }

                        if (trackSource.fromString(openedAudioSource.value) is trackSource.album) {
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
                        else {
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

                                Text(
                                    text = (playlist?.trackCount ?: 0).toString() + " songs",
                                    fontSize = 16.sp,
                                    color = Color(255, 255, 255, 100)
                                )

                                Spacer(Modifier.height(6.dp))


                            }
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

            drawTrack(
                num = num,
                item = item,
                audioSourceForItem = openedAudioSource.value,
                trackDropDownOpen = trackDropDownOpen,
                onPlay = {

                    grooviqCore.controllers.audioController.bassQueueController.buildFromSource(
                        tracks = openedAlbumTracks,
                        audioSource = openedAudioSource.value,
                        startTrack = item
                    )

                },
                onAddToPlaylist = {
                    targetTrackPopup.value = item
                }
            )
        }

        item {
            Spacer(Modifier.height(offsetOfBottomBar.value + 16.dp))
        }
    }

    handleTracksPopUp(
        targetTrackPopup,
        playlistController = playlistController
    )
}