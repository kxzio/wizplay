package org.example.ui.screens.leftPager.queue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.onClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBackIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.example.bass.queue.QueueItem
import org.example.bassQueueController
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf

@Composable
fun drawQueue() {


    Column(Modifier.fillMaxSize().padding(top = 98.dp)) {


        Row(modifier = Modifier.padding(start = 32.dp, end = 32.dp),
            verticalAlignment = Alignment.CenterVertically) {

            Row {

                wizui.wizBlinkingText(
                    "queue",
                    normalColor = Color(255, 255, 255),
                    blinkColor = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 12.dp),
                    onClick = {

                    }
                )

            }
        }


        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 32.dp),
            thickness = 1.0.dp,
            color = MaterialTheme.colorScheme.primary
        )

        val currentQueue = bassQueueController.queue

        val localQueue = remember {
            mutableStateListOf<QueueItem>()
        }

        LaunchedEffect(currentQueue) {
            localQueue.clear()
            localQueue.addAll(currentQueue)
        }

        val state = rememberReorderableLazyListState(
            onMove = { from, to ->
                val fromIdx = from.index
                val toIdx = to.index

                if (
                    fromIdx in localQueue.indices &&
                    toIdx in localQueue.indices &&
                    fromIdx != toIdx
                ) {
                    localQueue.add(toIdx, localQueue.removeAt(fromIdx))
                }
            },
            onDragEnd = { _, _ ->
                bassQueueController.setQueue(localQueue.toList())
            }
        )

        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .reorderable(state)

        ) {
            itemsIndexed(localQueue) { index, item ->

                ReorderableItem(state, index = index, key = item.id) { isDragging ->

                    wizui.wizButton(
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth().detectReorder(state),
                        contentColor = Color(255, 255, 255),
                        backgroundColor = Color(35, 35, 35, 0),
                        onClick = {

                        }
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Box(
                                modifier = Modifier
                                    .size(36.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                artworkAsync(
                                    item.track.artworkPath,
                                    Modifier.fillMaxSize()
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.padding(start = 16.dp).fillMaxWidth()
                            ) {

                                Text(
                                    item.track.title, fontSize = 16.sp,
                                    color = if (bassQueueController.isPlaying(item.track, item.audioSource))
                                        MaterialTheme.colorScheme.primary else Color.White
                                )

                                Spacer(Modifier.height(4.dp))
                                Text(item.track.artist, fontSize = 12.sp, color = Color(255, 255, 255, 100))
                            }
                        }

                    }
                }

            }
        }
    }


}