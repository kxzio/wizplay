package org.example.bass.queue

import androidx.compose.runtime.*
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

    // priority buffer for user "add next" — plays before permList continues
    private val userQueue = ArrayDeque<QueueItem>()

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

    private fun rebuildInvPerm() {
        invPerm = IntArray(canonical.size) { -1 }
        for (i in permList.indices) {
            val canonIdx = permList[i]
            if (canonIdx in invPerm.indices) invPerm[canonIdx] = i
        }
    }

    private fun ensurePermMatchesCanonical() {
        // Called after canonical changes in a way that requires perm to include new indices.
        if (permList.size != canonical.size) {
            // If perm is empty, initialize with identity
            permList.clear()
            for (i in canonical.indices) permList.add(i)
            rebuildInvPerm()
            updateVisibleSnapshot()
        }
    }

    private fun stableIdForSource(trackPath: String, index: Int) =
        "$trackPath::$index"

    private fun findCanonicalIndexByPathAndNearest(path: String, preferIndex: Int = 0): Int {
        // find the canonical index with same path and nearest to preferIndex (best-effort)
        var best = -1
        var bestDist = Int.MAX_VALUE
        for (i in canonical.indices) {
            if (canonical[i].track.path.toString() == path) {
                val dist = kotlin.math.abs(i - preferIndex)
                if (dist < bestDist) {
                    bestDist = dist
                    best = i
                }
            }
        }
        return best
    }

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
            // Called by player when it needs the next track
            // If userQueue exists -> serve from it first (we don't alter permList)
            if (userQueue.isNotEmpty()) {
                val it = userQueue.removeFirst()
                playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
            } else {
                // normal behaviour
                if (posInQueue + 1 >= permList.size) {
                    when (repeatMode) {
                        repeatMods.REPEAT_OFF -> null
                        repeatMods.REPEAT_ALL -> {
                            // reshuffle and restart
                            reshuffleIfShuffleEnabled()
                            posInQueue = 0
                            val canonIdx = permList.getOrNull(posInQueue) ?: null

                            if (canonIdx == null)
                                null

                            canonical[canonIdx!!].let {
                                playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
                            }
                        }
                        repeatMods.REPEAT_ONE -> {
                            val canonIdx = permList.getOrNull(posInQueue) ?: null

                            if (canonIdx == null)
                                null

                            canonical[canonIdx!!].let {
                                playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
                            }
                        }
                    }
                } else {
                    if (repeatMode == repeatMods.REPEAT_ONE) {
                        val canonIdx = permList.getOrNull(posInQueue) ?: null

                        if (canonIdx == null)
                            null

                        canonical[canonIdx!!].let {
                            playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
                        }
                    } else {
                        posInQueue++
                        val canonIdx = permList.getOrNull(posInQueue) ?: null

                        if (canonIdx == null)
                            null

                        canonical[canonIdx!!].let {
                            playlistItem(trackPath = it.track.path.toString(), audioSource = it.audioSource)
                        }
                    }
                }
            }
        }

        if (permList.isNotEmpty()) {
            playCurrent()
        }
    }

    fun playCurrent() {
        // userQueue priority
        if (userQueue.isNotEmpty()) {
            val item = userQueue.first()
            _currentTrackState.value = item.track
            player?.play(
                playlistItem(
                    trackPath = item.track.path.toString(),
                    audioSource = item.audioSource
                )
            )
            return
        }

        val canonIdx = permList.getOrNull(posInQueue) ?: return
        val item = canonical.getOrNull(canonIdx) ?: return

        _currentTrackState.value = item.track

        player?.play(
            playlistItem(
                trackPath = item.track.path.toString(),
                audioSource = item.audioSource
            )
        )
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
        // Quick path: same source and the startTrack exists in current visible -> jump
        if (currentSourceId == audioSource && canonical.isNotEmpty()) {
            // try find visible position by path equality (works for duplicates because ids are stable)
            val path = startTrack.path.toString()
            // find nearest canonical index matching same path
            val canonIdx = findCanonicalIndexByPathAndNearest(path, preferIndex = permList.getOrNull(posInQueue) ?: 0)
            if (canonIdx != -1) {
                val visible = invPerm.getOrNull(canonIdx) ?: -1
                if (visible >= 0) {
                    posInQueue = visible
                    playCurrent()
                    return
                }
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
        // if shuffle enabled, shuffle perm now
        if (isShuffle) fisherYatesShufflePerm()
        else rebuildInvPerm() // invPerm set accordingly

        currentSourceId = audioSource

        // set posInQueue to the startTrack (match by path first, then fallback to 0)
        val startPath = startTrack.path.toString()
        var startCanonIdx = canonical.indexOfFirst { it.track.path.toString() == startPath }
        if (startCanonIdx < 0) startCanonIdx = 0
        posInQueue = invPerm.getOrNull(startCanonIdx) ?: 0

        updateVisibleSnapshot()
        playCurrent()
    }

    // ───────────── SHUFFLE / RESHUFFLE ─────────────

    fun toggleShuffle(enable: Boolean) {
        if (enable == isShuffle) return
        if (permList.isEmpty()) return

        val currentCanon = permList.getOrNull(posInQueue)
        isShuffle = enable

        if (enable) {
            // shuffle permList but keep current element at position 0 to continue playback
            fisherYatesShufflePerm()
            // move currentCanon to front if present
            if (currentCanon != null) {
                val idx = permList.indexOf(currentCanon)
                if (idx > 0) {
                    val e = permList.removeAt(idx)
                    permList.add(0, e)
                }
                posInQueue = 0
                rebuildInvPerm()
            }
        } else {
            // return to canonical order: identity perm
            permList.clear()
            for (i in canonical.indices) permList.add(i)
            // put current canon at its canonical position index
            val idx = currentCanon ?: 0
            posInQueue = idx.coerceAtMost(permList.lastIndex.coerceAtLeast(0))
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
     * We both push it to userQueue for playback priority (O(1)) and insert into the visible order
     * so that UI shows it. Insertion into permList is O(n) (shifting) but it's a single small cost.
     */
    fun addNext(track: ScannedAudio, source: String) {
        val qi = QueueItem(track = track, audioSource = source, addedByUser = true, id = UUID.randomUUID().toString())
        // priority buffer
        userQueue.addLast(qi)

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

        // Attempt to maintain canonical order similar to original: move corresponding canonical entry
        val origIdx = canonical.indexOfFirst { it.id == canonical.getOrNull(elemCanon)?.id }
        if (origIdx != -1) {
            // move canonical entry to approximately same 'to' position (clamped)
            val targetPos = to.coerceIn(0, canonical.size - 1)
            val item = canonical.removeAt(origIdx)
            canonical.add(targetPos, item)
            // fix permList indices to reflect new canonical positions (re-index)
            // Because canonical changed, we must rebuild idToCanonicalIdx and remap permList
            idToCanonicalIdx.clear()
            for (i in canonical.indices) idToCanonicalIdx[canonical[i].id] = i
            for (i in permList.indices) {
                val id = canonical.getOrNull(permList[i])?.id
                // map old canonical idx value to new index via id map
                if (id != null) {
                    permList[i] = idToCanonicalIdx[id] ?: permList[i]
                }
            }
        }

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
        // userQueue first
        if (userQueue.isNotEmpty()) {
            val next = userQueue.removeFirst()
            player?.play(playlistItem(trackPath = next.track.path.toString(), audioSource = next.audioSource))
            return true
        }

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

    // ───────────── GETTERS / UTIL ─────────────

    fun currentItem(): QueueItem? = canonical.getOrNull(permList.getOrNull(posInQueue) ?: -1)
    fun currentTrack(): ScannedAudio? = _currentTrackState.value
    fun isPlaying(track: ScannedAudio): Boolean {
        val cur = currentTrack() ?: return false
        return cur.path.toString() == track.path.toString()
    }
}
