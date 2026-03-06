package org.example.ui.screens.rightPager.tracklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.Folder
import androidx.compose.material.icons.sharp.LibraryAdd
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.coreMaster.grooviqCore
import org.example.audioindex.ScannedAudio
import org.example.openFileInFileManager


@Composable
fun createDropDownTrack(
    offsetFromIntToDp: DpOffset,
    expanded: Boolean,
    scannedAudio: ScannedAudio,
    audioSourceFrom: String,
    onDismissRequest: () -> Unit,
    onPlaySelected: () -> Unit,
    onAddToPlaylistSelected: () -> Unit,
)
{
    DropdownMenu(
        shape = RectangleShape,
        offset = offsetFromIntToDp,
        containerColor = Color(20, 20, 20),
        border = BorderStroke(0.5.dp, Color(255, 255, 255, 50)),
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(220.dp).padding(horizontal = 8.dp)
    ) {

        Text(scannedAudio.artist + " - " + scannedAudio.title,
            fontSize = 12.sp,
            color = Color(255, 255, 255, 100),
            overflow = TextOverflow.Ellipsis
        )

        DropdownMenuItem(
            text = {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(Icons.Sharp.PlayArrow, "",
                        tint = Color(255, 255, 255, 255))

                    Text(
                        "play now",
                        modifier = Modifier.padding(start = 12.dp),
                        color = Color(255, 255, 255, 255)
                    )
                }

            },
            onClick = {
                onPlaySelected()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(Icons.Sharp.LibraryAdd, "",
                        tint = Color(255, 255, 255, 255))

                    Text(
                        "play next",
                        modifier = Modifier.padding(start = 12.dp),
                        color = Color(255, 255, 255, 255)
                    )
                }
            },
            onClick = {
                onDismissRequest()
                grooviqCore.controllers.audioController.bassQueueController.addNext(scannedAudio, audioSourceFrom)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DropdownMenuItem(
            text = {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        Icons.AutoMirrored.Sharp.PlaylistAdd, "",
                        tint = Color(255, 255, 255, 255))

                    Text(
                        "add to playlist",
                        modifier = Modifier.padding(start = 12.dp),
                        color = Color(255, 255, 255, 255)
                    )
                }
            },
            onClick = {
                onDismissRequest()
                onAddToPlaylistSelected()
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DropdownMenuItem(
            text = {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        Icons.Sharp.Folder, "",
                        tint = Color(255, 255, 255, 255))

                    Text(
                        "open in file manager",
                        modifier = Modifier.padding(start = 12.dp),
                        color = Color(255, 255, 255, 255)
                    )
                }
            },
            onClick = {
                openFileInFileManager(scannedAudio.path)
            }
        )
    }
}