package org.example.bass.queue

import androidx.compose.runtime.*
import org.example.MprisService
import org.example.OS
import org.example.audioindex.ScannedAudio
import org.example.bass.bassController.PlayerController
import org.example.bass.bassController.playlistItem
import java.util.UUID
import kotlin.random.Random

data class QueueItem(
    val track: ScannedAudio,
    val audioSource: String,
    val addedByUser: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)

enum class repeatMods {
    REPEAT_OFF,
    REPEAT_ALL,
    REPEAT_ONE
}

class QueueController {

    //linux media controller
    private var mpris: MprisService? = null

    // ───────────── INTERNAL STRUCTURES ─────────────
    // canonical list (original order for the source). Not a Compose list -> avoid massive recompositions.
    private val canonical = ArrayList<QueueItem>()
    // visible order as indices into canonical
    private val permList: MutableList<Int> = ArrayList()
    // inverse mapping canonicalIdx -> visible pos (kept up-to-date when permList changes)
    private var invPerm: IntArray = IntArray(0)
    // cached visible snapshot for UI (only updated when order/content changes)
    private val visibleSnapshotState = mutableStateOf<List<QueueItem>>(emptyList())
    // pos in visible order
    var posInQueue by mutableStateOf(0)
        private set
    var isShuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(repeatMods.REPEAT_OFF)
        private set
    // Short alias to expose queue to old UI code. Returns cached snapshot.
    val queue: List<QueueItem> get() = visibleSnapshotState.value
    // quick lookup id -> canonical index
    private val idToCanonicalIdx = HashMap<String, Int>()
    private var savedPermBeforeShuffle: List<String>? = null
    // source tracking
    private var currentSourceId: String? = null
    // bound player
    private var player: PlayerController? = null
    private val _currentTrackState = mutableStateOf<ScannedAudio?>(null)
    // --- helpers ------------------------------------------------

    private fun updateVisibleSnapshot() {
        // create a new list mapped by permList
        val list = ArrayList<QueueItem>(permList.size)
        for (idx in permList) {
            // safety check (in case canonical changed unexpectedly)
            if (idx in canonical.indices) list.add(canonical[idx])
        }
        visibleSnapshotState.value = list
    }

    fun setQueue(newVisible: List<QueueItem>) {
        // 1. Запоминаем текущий playing item
        val currentItemId =
            permList.getOrNull(posInQueue)
                ?.let { canonical.getOrNull(it)?.id }
        // 2. Строим новый permList
        permList.clear()
        for (item in newVisible) {
            idToCanonicalIdx[item.id]?.let { permList.add(it) }
        }
        // 3. Восстанавливаем posInQueue по ID
        posInQueue =
            if (currentItemId != null) {
                permList.indexOfFirst { canonical[it].id == currentItemId }
                    .takeIf { it >= 0 } ?: 0
            } else 0
        rebuildInvPerm()
        updateVisibleSnapshot()
    }

    private fun rebuildInvPerm() {
        invPerm = IntArray(canonical.size) { -1 }
        for (i in permList.indices) {
            val canonIdx = permList[i]
            if (canonIdx in invPerm.indices) invPerm[canonIdx] = i
        }
    }

    private fun stableIdForSource(trackPath: String, index: Int) =
        "$trackPath::$index"
    // Fisher-Yates shuffle on permList

    private fun fisherYatesShufflePerm(random: Random = Random.Default) {
        val n = permList.size
        for (i in n - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = permList[i]
            permList[i] = permList[j]
            permList[j] = tmp
        }
        rebuildInvPerm()
    }
    // ───────────── PLAYER ATTACH / PLAY ─────────────

    fun attachPlayer(player: PlayerController) {
        this.player = player
        player.requestNextItem = {
            // normal behaviour
            if (posInQueue + 1 >= permList.size) {
                when (repeatMode) {
                    repeatMods.REPEAT_OFF -> null
                    repeatMods.REPEAT_ALL -> {
                        // reshuffle and restart
                        reshuffleIfShuffleEnabled()
                        posInQueue = 0
                        val canonIdx = permList.getOrNull(posInQueue) ?: null
                        val it = canonical[canonIdx!!]
                        _currentTrackState.value = it.track
                        playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
                    }
                    repeatMods.REPEAT_ONE -> {
                        val canonIdx = permList.getOrNull(posInQueue) ?: null
                        val it = canonical[canonIdx!!]
                        _currentTrackState.value = it.track
                        playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
                    }
                }
            } else {
                if (repeatMode == repeatMods.REPEAT_ONE) {
                    val canonIdx = permList.getOrNull(posInQueue) ?: null
                    val it = canonical[canonIdx!!]
                    _currentTrackState.value = it.track
                    playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
                } else {
                    posInQueue++
                    val canonIdx = permList.getOrNull(posInQueue) ?: null
                    val it = canonical[canonIdx!!]
                    _currentTrackState.value = it.track
                    playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
                }
            }
        }

        if (OS.isLinux) {
            mpris = MprisService(player)
        }

        if (permList.isNotEmpty()) {
            playCurrent()
        }
    }

    fun playCurrent() {
        val canonIdx = permList.getOrNull(posInQueue) ?: return
        val item = canonical.getOrNull(canonIdx) ?: return
        _currentTrackState.value = item.track
        player?.play(
            playlistItem(
                trackPath = item.track.path.toString(),
                audioSource = item.audioSource
            )
        )

        mpris?.updateFullMetadata()
    }

    private fun isIdentityPerm(): Boolean {
        if (permList.size != canonical.size) return false
        for (i in permList.indices) {
            if (permList[i] != i) return false
        }
        return true
    }
    // ───────────── BUILD / REBUILD ─────────────
    /**
     * Build the queue from a source. If same source and startTrack is already in visible order,
     * simply jump to it (O(1)). Otherwise do a full rebuild (O(n)).
     *
     * audioSourceId - stable id for the source (album/playlist/folder)
     */

    fun buildFromSource(
        tracks: List<ScannedAudio>,
        audioSource: String,
        startTrack: ScannedAudio
    ) {
        val canFastJump =
            currentSourceId == audioSource &&
                    canonical.size == tracks.size &&
                    isIdentityPerm() &&
                    canonical.indices.all { i ->
                        canonical[i].track.path == tracks[i].path
                    }
        if (canFastJump) {
            val path = startTrack.path.toString()
            val canonIdx = canonical.indexOfFirst {
                it.track.path.toString() == path
            }
            if (canonIdx != -1) {
                posInQueue = canonIdx
                playCurrent()
                return
            }
        }
        // Full rebuild (heavy path) — construct stable canonical items
        canonical.clear()
        idToCanonicalIdx.clear()
        for ((i, t) in tracks.withIndex()) {
            val stableId = stableIdForSource(t.path.toString(), i)
            val qi = QueueItem(track = t, audioSource = audioSource, addedByUser = false, id = stableId)
            canonical.add(qi)
            idToCanonicalIdx[qi.id] = i
        }
        // rebuild perm
        permList.clear()
        for (i in canonical.indices) permList.add(i)
        // find startTrack canonical index
        val startPath = startTrack.path.toString()
        var startCanonIdx = canonical.indexOfFirst { it.track.path.toString() == startPath }
        if (startCanonIdx < 0) startCanonIdx = 0
        // if shuffle enabled, shuffle perm now and move startTrack to front
        if (isShuffle) {
            fisherYatesShufflePerm()
            // Move startCanonIdx to the beginning of permList (if not already)
            val idxInPerm = permList.indexOf(startCanonIdx)
            if (idxInPerm > 0) {
                permList.removeAt(idxInPerm)
                permList.add(0, startCanonIdx)
                rebuildInvPerm() // Update invPerm after move
            }
            posInQueue = 0
        } else {
            rebuildInvPerm()
            posInQueue = invPerm.getOrNull(startCanonIdx) ?: 0
        }
        currentSourceId = audioSource
        updateVisibleSnapshot()
        playCurrent()
    }
    // ───────────── SHUFFLE / RESHUFFLE ─────────────

    fun toggleShuffle(enable: Boolean) {
        if (enable == isShuffle) return
        if (permList.isEmpty()) return
        val currentCanon = permList.getOrNull(posInQueue)
        if (enable) {
            // 🔒 сохранить текущий порядок как список ID
            savedPermBeforeShuffle = permList.map { canonical[it].id }
            isShuffle = true
            fisherYatesShufflePerm()
            // текущий трек — в начало
            if (currentCanon != null) {
                val idx = permList.indexOf(currentCanon)
                if (idx > 0) {
                    permList.removeAt(idx)
                    permList.add(0, currentCanon)
                }
                posInQueue = 0
                rebuildInvPerm()
            }
        } else {
            // 🔓 восстановить порядок
            val restored = savedPermBeforeShuffle
            if (restored != null) {
                permList.clear()
                val restoredSet = restored.toSet()
                // добавить старые в порядке restored, если еще существуют
                for (id in restored) {
                    idToCanonicalIdx[id]?.let { permList.add(it) }
                }
                // добавить новые (не в restored) в порядке их canonical индексов
                val newIdxs = canonical.indices.filter { canonical[it].id !in restoredSet }.sorted()
                for (i in newIdxs) permList.add(i)
            } else {
                // fallback (на всякий)
                permList.clear()
                for (i in canonical.indices) permList.add(i)
            }
            isShuffle = false
            savedPermBeforeShuffle = null
            // восстановить позицию текущего трека
            posInQueue =
                currentCanon?.let { permList.indexOf(it) }
                    ?.takeIf { it >= 0 } ?: 0
            rebuildInvPerm()
        }
        updateVisibleSnapshot()
    }

    fun reshuffleIfShuffleEnabled() {
        if (!isShuffle || permList.isEmpty()) return
        val currentCanon = permList.getOrNull(posInQueue)
        fisherYatesShufflePerm()
        // put current canon to front
        if (currentCanon != null) {
            val idx = permList.indexOf(currentCanon)
            if (idx > 0) {
                val e = permList.removeAt(idx)
                permList.add(0, e)
            }
            posInQueue = 0
            rebuildInvPerm()
        }
        updateVisibleSnapshot()
    }

    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            repeatMods.REPEAT_OFF -> repeatMods.REPEAT_ALL
            repeatMods.REPEAT_ALL -> repeatMods.REPEAT_ONE
            repeatMods.REPEAT_ONE -> repeatMods.REPEAT_OFF
        }
    }
    // ───────────── ADD NEXT ─────────────
    /**
     * Add a track to be played next. This operation aims to be fast (amortized).
     * We insert into the visible order so that UI shows it. Insertion into permList is O(n) (shifting) but it's a single small cost.
     */

    fun addNext(track: ScannedAudio, source: String) {
        val qi = QueueItem(track = track, audioSource = source, addedByUser = true, id = UUID.randomUUID().toString())
        // create canonical entry and insert roughly after current visible position
        val newCanonIdx = canonical.size
        canonical.add(qi)
        idToCanonicalIdx[qi.id] = newCanonIdx
        // compute visible insertion position: after current pos, but after any other addedByUser items that follow
        val after = posInQueue + 1
        var insertPos = after.coerceIn(0, permList.size)
        // walk forward to skip over elements that are addedByUser (preserve grouping)
        while (insertPos < permList.size) {
            val cIdx = permList[insertPos]
            if (canonical.getOrNull(cIdx)?.addedByUser == true) insertPos++ else break
        }
        // insert new canonical index into permList at that visible position
        permList.add(insertPos.coerceIn(0, permList.size), newCanonIdx)
        // rebuild invPerm & snapshot
        rebuildInvPerm()
        updateVisibleSnapshot()
    }
    // ───────────── REMOVE ─────────────

    fun removeAt(index: Int) {
        if (index !in permList.indices) return
        val canonIdx = permList[index]
        val target = canonical.getOrNull(canonIdx) ?: return
        // remove from visible perm
        permList.removeAt(index)
        // remove from canonical (original) - we need to update indices in permList accordingly
        // Find its canonical index in canonical list and remove it
        val removedCanonIdx = canonical.indexOfFirst { it.id == target.id }
        if (removedCanonIdx != -1) {
            canonical.removeAt(removedCanonIdx)
            idToCanonicalIdx.remove(target.id)
            // decrement canonical indices in permList greater than removedCanonIdx
            for (i in permList.indices) {
                val v = permList[i]
                if (v > removedCanonIdx) permList[i] = v - 1
            }
        }
        // adjust posInQueue
        if (posInQueue > index) posInQueue--
        if (posInQueue >= permList.size) posInQueue = permList.lastIndex.coerceAtLeast(0)
        // rebuild inv perm & snapshot
        rebuildInvPerm()
        updateVisibleSnapshot()
    }
    // ───────────── MOVE ─────────────

    fun move(from: Int, to: Int) {
        if (from !in permList.indices) return
        if (to !in permList.indices) return
        val elemCanon = permList.removeAt(from)
        permList.add(to, elemCanon)
        // adjust posInQueue based on move
        posInQueue = when {
            posInQueue == from -> to
            from < posInQueue && to >= posInQueue -> posInQueue - 1
            from > posInQueue && to <= posInQueue -> posInQueue + 1
            else -> posInQueue
        }.coerceIn(0, permList.lastIndex.coerceAtLeast(0))
        // rebuild inv and visible snapshot
        rebuildInvPerm()
        updateVisibleSnapshot()
    }
    // ───────────── NEXT / PREV ─────────────

    fun moveNext(isAutoTransition: Boolean = false): Boolean {
        if (posInQueue + 1 >= permList.size) {
            when (repeatMode) {
                repeatMods.REPEAT_OFF -> return false
                repeatMods.REPEAT_ALL -> {
                    reshuffleIfShuffleEnabled()
                    posInQueue = 0
                    playCurrent()
                    return true
                }
                repeatMods.REPEAT_ONE -> {
                    if (isAutoTransition) {
                        playCurrent()
                        return true
                    } else return false
                }
            }
        }
        if (repeatMode == repeatMods.REPEAT_ONE && isAutoTransition) {
            playCurrent()
            return true
        }
        posInQueue++
        playCurrent()
        return true
    }

    fun movePrev(): Boolean {
        if (posInQueue - 1 < 0) return false
        posInQueue--
        playCurrent()
        return true
    }

    fun moveToNewPosInQueueById(idOfElement : String): Boolean {
        val newPosInQueue = queue.indexOfFirst { it.id == idOfElement }
        if (newPosInQueue != -1) {
            posInQueue = newPosInQueue
            playCurrent()
            return true
        }
        return false
    }


    // ───────────── GETTERS / UTIL ─────────────
    fun currentItem(): QueueItem? = canonical.getOrNull(permList.getOrNull(posInQueue) ?: -1)
    fun currentTrack(): ScannedAudio? = _currentTrackState.value
    fun isPlaying(track: ScannedAudio, audioSource: String): Boolean {
        val curTrack = currentTrack() ?: return false
        val curItem = currentItem() ?: return false
        return curTrack.path.toString() == track.path.toString() && curItem.audioSource == audioSource
    }
}