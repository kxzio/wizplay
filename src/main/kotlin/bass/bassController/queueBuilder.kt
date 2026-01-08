package org.example.bass.queue

import androidx.compose.runtime.*
import org.example.audioindex.ScannedAudio
import org.example.bass.bassController.PlayerController
import org.example.bass.bassController.playlistItem
import java.util.UUID

/* ───────────── QUEUE ITEM ───────────── */

data class QueueItem(
    val track: ScannedAudio,
    val audioSource: String,
    val addedByUser: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)

/* ───────────── QUEUE CONTROLLER ───────────── */

class QueueController {

    /* canonical order */
    private val originalQueue = mutableStateListOf<QueueItem>()

    /* visible / shuffled order */
    private val currentQueue = mutableStateListOf<QueueItem>()

    var posInQueue by mutableStateOf(0)
        private set

    var isShuffle by mutableStateOf(false)
        private set

    val queue: List<QueueItem> get() = currentQueue

    /* ───────────── BUILD QUEUE ───────────── */

    fun buildFromSource(
        tracks: List<ScannedAudio>,
        audioSource: String,
        startTrack: ScannedAudio
    ) {
        val base = tracks.map {
            QueueItem(track = it, audioSource = audioSource)
        }

        originalQueue.clear()
        originalQueue.addAll(base)

        rebuild(startTrack)
    }

    private fun rebuild(startTrack: ScannedAudio) {
        currentQueue.clear()

        if (isShuffle) {
            val shuffled = originalQueue.shuffled().toMutableList()
            val idx = shuffled.indexOfFirst { it.track == startTrack }
            if (idx > 0) {
                val elem = shuffled.removeAt(idx)
                shuffled.add(0, elem)
            }
            currentQueue.addAll(shuffled)
            posInQueue = 0
        } else {
            currentQueue.addAll(originalQueue)
            posInQueue = originalQueue.indexOfFirst { it.track == startTrack }
                .coerceAtLeast(0)
        }
    }

    /* ───────────── SHUFFLE ───────────── */

    fun toggleShuffle(enable: Boolean) {
        if (enable == isShuffle) return
        if (currentQueue.isEmpty()) return

        val current = currentQueue[posInQueue]
        isShuffle = enable

        if (enable) {
            val shuffled = currentQueue.shuffled().toMutableList()
            val idx = shuffled.indexOfFirst { it.id == current.id }
            if (idx > 0) {
                val elem = shuffled.removeAt(idx)
                shuffled.add(0, elem)
            }
            currentQueue.clear()
            currentQueue.addAll(shuffled)
            posInQueue = 0
        } else {
            val idx = originalQueue.indexOfFirst { it.id == current.id }
            currentQueue.clear()
            currentQueue.addAll(originalQueue)
            posInQueue = idx.coerceAtLeast(0)
        }
    }

    /* ───────────── ADD NEXT ───────────── */

    fun addNext(track: ScannedAudio, source: String) {
        val elem = QueueItem(track, source, addedByUser = true)

        val insertPos = posInQueue + 1 +
                currentQueue.drop(posInQueue + 1).count { it.addedByUser }

        currentQueue.add(insertPos, elem)

        val origInsert = posInQueue + 1 +
                originalQueue.drop(posInQueue + 1).count { it.addedByUser }

        originalQueue.add(origInsert, elem)
    }

    /* ───────────── REMOVE ───────────── */

    fun removeAt(index: Int) {
        if (index !in currentQueue.indices) return

        val target = currentQueue[index]
        currentQueue.removeAt(index)
        originalQueue.removeAll { it.id == target.id }

        if (posInQueue > index) posInQueue--
        if (posInQueue >= currentQueue.size)
            posInQueue = currentQueue.lastIndex.coerceAtLeast(0)
    }

    /* ───────────── MOVE ───────────── */

    fun move(from: Int, to: Int) {
        if (from !in currentQueue.indices) return
        if (to !in currentQueue.indices) return

        val elem = currentQueue.removeAt(from)
        currentQueue.add(to, elem)

        val origFrom = originalQueue.indexOfFirst { it.id == elem.id }
        if (origFrom != -1) {
            originalQueue.removeAt(origFrom)
            originalQueue.add(to.coerceIn(0, originalQueue.size), elem)
        }

        posInQueue = when {
            posInQueue == from -> to
            from < posInQueue && to >= posInQueue -> posInQueue - 1
            from > posInQueue && to <= posInQueue -> posInQueue + 1
            else -> posInQueue
        }
    }

    /* ───────────── NEXT / PREV ───────────── */

    fun moveNext(): Boolean {
        if (posInQueue + 1 >= currentQueue.size) return false
        posInQueue++
        return true
    }

    fun movePrev(): Boolean {
        if (posInQueue - 1 < 0) return false
        posInQueue--
        return true
    }

    /* ───────────── SYNC WITH PLAYER ───────────── */

    fun syncPlayer(player: PlayerController) {
        if (currentQueue.isEmpty()) return

        val playlist = currentQueue.map {
            playlistItem(it.track, it.audioSource)
        }

        player.playQueue(playlist, posInQueue)
    }
}
