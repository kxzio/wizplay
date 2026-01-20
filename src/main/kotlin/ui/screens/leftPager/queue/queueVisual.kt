package org.example.ui.screens.leftPager.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.DragHandle
import androidx.compose.material.icons.sharp.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.example.bass.queue.QueueItem
import org.example.bassQueueController
import org.example.ui.screens.leftPager.albums.artworkAsync
import org.example.wizui.wizui

@Composable
fun drawQueue(offsetOfBottomBar: MutableState<Dp>) {


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
        val currentIndex = bassQueueController.posInQueue

        val localQueue = remember { mutableStateListOf<QueueItem>() }
        val visualQueue = remember { mutableStateListOf<QueueItem>() }

        LaunchedEffect(currentQueue, currentIndex) {

            localQueue.clear()
            localQueue.addAll(currentQueue)

            visualQueue.clear()

            if (currentQueue.isNotEmpty() && currentIndex in currentQueue.indices) {
                val end = minOf(currentIndex + 100, currentQueue.size)
                visualQueue.addAll(
                    currentQueue.subList(currentIndex, end)
                )
            }
        }

        val state = rememberReorderableLazyListState(
            onMove = { from, to ->

                if (from.index == to.index) return@rememberReorderableLazyListState

                // Запрет: нельзя двигать текущий трек (index 0 в visual)
                if (from.index == 0) return@rememberReorderableLazyListState

                // Запрет: нельзя вставить выше текущего (to.index < 1)
                if (to.index < 1) return@rememberReorderableLazyListState

                val windowOffset = bassQueueController.posInQueue

                val fromGlobalIdx = windowOffset + from.index
                val toGlobalIdx = windowOffset + to.index

                if (fromGlobalIdx in localQueue.indices && toGlobalIdx in localQueue.indices) {
                    // Swap in full local queue (for correctness)
                    localQueue.add(toGlobalIdx, localQueue.removeAt(fromGlobalIdx))
                    // Also swap in visible queue (for immediate UI preview during drag)
                    visualQueue.add(to.index, visualQueue.removeAt(from.index))
                }
            },
            onDragEnd = { _, _ ->
                // Commit changes to controller
                bassQueueController.setQueue(localQueue.toList())
            }
        )

        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .reorderable(state)
        ) {
            itemsIndexed(visualQueue, key = { _, item -> item.id }, contentType = { _, _ -> "track" }) { index, item ->

                ReorderableItem(state, key = item.id) { isDragging ->

                    wizui.wizButton(
                        shape = RectangleShape,

                        modifier = Modifier.fillMaxWidth().then(if (index != 0) Modifier.detectReorder(state) else Modifier),
                        contentColor = Color(255, 255, 255),
                        backgroundColor = Color(35, 35, 35, 0),
                        onClick = {
                            bassQueueController.moveToNewPosInQueueById(item.id)
                        }
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {

                                IconButton(    onClick = {

                                    // index — это индекс в visualQueue
                                    // index 0 = текущий трек → запрещаем
                                    if (index == 0) return@IconButton

                                    val windowOffset = bassQueueController.posInQueue
                                    val globalIndex = windowOffset + index

                                    if (globalIndex !in localQueue.indices) return@IconButton

                                    // удалить из полной очереди
                                    localQueue.removeAt(globalIndex)

                                    // удалить из визуального окна
                                    visualQueue.removeAt(index)

                                    // закоммитить изменения
                                    bassQueueController.setQueue(localQueue.toList())
                                })
                                {
                                    Icon(Icons.Sharp.Close, "",
                                        tint = Color(255, 255, 255, if (index == 0) 0 else 100))
                                }

                                Spacer(Modifier.width(6.dp))

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

                            Icon(Icons.Sharp.DragHandle, "", tint = Color(255, 255, 255, if (index == 0) 0 else 100))
                        }


                    }
                }

            }

            item {
                Spacer(Modifier.height(offsetOfBottomBar.value + 16.dp))
            }
        }
    }

}