package org.example.ui.screens.leftPager.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.folderGetter.FolderScanController
import org.example.folderGetter.FolderScanState
import org.example.folderGetter.ScannedFolder
import org.example.pickFolderWindowsNative
import org.example.wizui.wizui
import java.nio.file.Path

@Composable
fun folderScanContent(
    folderScanController: FolderScanController,
    folders: List<ScannedFolder>,
    onAddFolder: (Path) -> Unit,
    onRemoveFolder: (Path) -> Unit
) {

    if (!folders.isEmpty())
    wizui.wizVerticalList(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(30, 30, 30)),
        items = folders,
    ) { item ->

        val isReady = item.state is FolderScanState.Ready
        val isRefreshing = item.state is FolderScanState.Refreshing
        val isPreparing = item.state is FolderScanState.Preparing
        val isError = item.state is FolderScanState.Error

        wizui.wizButton(
            delayedClick = true,
            delayedClickDurationMs = 300,
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth(),
            contentColor =
                when {
                    isReady -> Color.White
                    isPreparing -> Color(255, 255, 255, 120)
                    isRefreshing -> Color(255, 255, 255, 120)
                    isError -> Color.Red
                    else -> Color.White
                },
            backgroundColor = Color(35, 35, 35),
            onClick = { /* optional */ }
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (item.state is FolderScanState.Preparing || item.state is FolderScanState.Refreshing)
                    {
                        Box(Modifier.padding(start = 16.dp)) {
                            when (val state = item.state) {

                                is FolderScanState.Preparing -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }

                                is FolderScanState.Refreshing -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }

                                else -> Unit
                            }
                        }

                    }

                    Text(
                        text = item.path.toString(),
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start =
                                    16.dp
                            )
                            .weight(1f),
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = {
                            folderScanController.refreshFolder(
                                item.path,
                            )
                        },
                        enabled = item.state !is FolderScanState.Refreshing && !isPreparing

                    ) {
                        Icon(Icons.Sharp.Refresh, contentDescription = "Refresh folder")
                    }

                    Icon(
                        Icons.Sharp.Close,
                        contentDescription = "",
                        Modifier.clickable {
                            onRemoveFolder(item.path)
                        }
                    )
                }



            }
        }
    }

    Spacer(Modifier.height(12.dp))

    wizui.wizButton(
        modifier = Modifier.fillMaxWidth(),
        contentColor = Color.White,
        shape = RectangleShape,
        onClick = {
            val folder = pickFolderWindowsNative()
            folder?.let {
                onAddFolder(it.toPath())
            }
        }
    ) {
        Text("add folders")
    }

    Spacer(Modifier.height(12.dp))

    Text("if you can't find your files - activate [update folders] and restart the application",
        color = Color(255, 255, 255, 100),
        fontSize = 11.sp
    )



}