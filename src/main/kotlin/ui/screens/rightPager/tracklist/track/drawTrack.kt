package org.example.ui.screens.rightPager.tracklist.track

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.coreMaster.grooviqCore
import org.example.audioindex.ScannedAudio
import org.example.dashedBorder
import org.example.ui.screens.leftPager.playlists.dropDownMenuOpenMode
import org.example.ui.screens.leftPager.playlists.openMode
import org.example.ui.screens.rightPager.tracklist.createDropDownTrack
import org.example.wizui.wizui

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun drawTrack(
    num: Int,
    audioSourceForItem: String,
    item: ScannedAudio,
    trackDropDownOpen: dropDownMenuOpenMode,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit
)
{
    wizui.wizButton(
        shape = RectangleShape,
        modifier = Modifier
            .fillMaxWidth()
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.buttons.isSecondaryPressed) {

                    val pos = event.changes.first().position
                    val intOffset = IntOffset(pos.x.toInt(), pos.y.toInt())

                    trackDropDownOpen.openDropDown(
                        openIndexName = item.path.toString(),
                        offset = intOffset,
                        openModeForSource = openMode.CURSOR_OPENED
                    )

                }
            }.dashedBorder(
                1.dp,
                color = if (trackDropDownOpen.openedIndexName == item.path.toString())
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else Color(0, 0, 0, 0)
            ),
        contentColor = Color(255, 255, 255),
        backgroundColor = Color(35, 35, 35, 0),
        onClick = {

            onPlay()


        }
    ) {

        val density = LocalDensity.current
        val offsetFromIntToDp = with (density) {
            DpOffset(x = trackDropDownOpen.intOffset.x.toDp(),
                y = 0.dp
            )
        }

        createDropDownTrack(
            offsetFromIntToDp = offsetFromIntToDp,
            expanded =
                trackDropDownOpen.openedIndexName == item.path.toString() &&
                        trackDropDownOpen.open == openMode.CURSOR_OPENED,
            onDismissRequest = { trackDropDownOpen.closeDropDown() },
            scannedAudio = item,
            audioSourceFrom = audioSourceForItem,
            onPlaySelected = {
                onPlay()
            },
            onAddToPlaylistSelected = onAddToPlaylist
        )

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
                modifier = Modifier.padding(start = 16.dp).weight(1f)
            ) {

                Text(item.title, fontSize = 16.sp,
                    color = if (grooviqCore.controllers.audioController.bassQueueController.isPlaying(item, item.albumKey))
                        MaterialTheme.colorScheme.primary else Color.White)

                Spacer(Modifier.height(4.dp))
                Text(item.artist, fontSize = 12.sp, color = Color(255, 255, 255, 100))
            }

            Box {

                IconButton(onClick = {
                    trackDropDownOpen.openDropDown(
                        openIndexName = item.path.toString(),
                        openModeForSource = openMode.BUTTON_OPENED
                    )
                })
                {
                    Icon(Icons.Sharp.MoreVert, "",
                        tint = Color(255, 255, 255, 100))
                }

                createDropDownTrack(
                    offsetFromIntToDp = DpOffset(0.dp, 0.dp),
                    expanded =
                        trackDropDownOpen.openedIndexName == item.path.toString() &&
                                trackDropDownOpen.open == openMode.BUTTON_OPENED,
                    scannedAudio = item,
                    onAddToPlaylistSelected = onAddToPlaylist,
                    audioSourceFrom = audioSourceForItem,
                    onPlaySelected = {
                        onPlay()
                    },
                    onDismissRequest = { trackDropDownOpen.closeDropDown() },
                )


            }

        }

    }
}