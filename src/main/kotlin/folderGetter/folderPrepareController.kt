package org.example.folderGetter

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.audioindex.AudioFolderController
import java.nio.file.Path
import kotlin.io.path.Path

sealed class FolderScanState {
    object Preparing : FolderScanState()
    data class Refreshing(
        val current: Int,
        val total: Int
    ) : FolderScanState()

    object Ready : FolderScanState()
    data class Error(val message: String) : FolderScanState()
}

data class ScannedFolder(
    val path: Path,
    val state: FolderScanState
)

class FolderScanController(
    private val audioController: AudioFolderController
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _folders =
        MutableStateFlow<List<ScannedFolder>>(emptyList())
    val folders: StateFlow<List<ScannedFolder>> = _folders

    private val scanQueue = ArrayDeque<Path>()
    private var scanning = false


    private fun enqueueScan(path: Path) {
        if (scanQueue.contains(path)) return
        scanQueue.addLast(path)

        if (!scanning) {
            scanning = true
            scope.launch { processQueueInternal() }
        }
    }

    private suspend fun processQueueInternal() {
        while (scanQueue.isNotEmpty()) {
            val path = scanQueue.removeFirst()

            updateState(path) {
                FolderScanState.Refreshing(0, 0)
            }

            try {
                audioController.refreshRoot(path) { current, total ->
                    updateState(path) {
                        FolderScanState.Refreshing(current, total)
                    }
                }

                updateState(path) { FolderScanState.Ready }

            } catch (e: Exception) {
                updateState(path) {
                    FolderScanState.Error(e.message ?: "Scan error")
                }
            }
        }

        scanning = false
    }

    fun addFolder(path: Path) {
        if (_folders.value.any { it.path == path }) return

        _folders.value += ScannedFolder(path, FolderScanState.Preparing)

        scope.launch {
            try {
                audioController.addRoot(path)
                markReady(path)
            } catch (e: Exception) {
                markError(path, e.message ?: "Unknown error")
            }
        }
    }

    fun refreshFolder(path: Path) {
        enqueueScan(path)
    }

    fun restoreFromAudioController() {
        _folders.value =
            audioController.getRoots().map {
                ScannedFolder(it, FolderScanState.Ready)
            }
    }

    fun removeFolder(path: Path) {
        scanQueue.remove(path)
        _folders.value = _folders.value.filterNot { it.path == path }
        audioController.removeRoot(path)
    }

    private fun markReady(path: Path) {
        updateState(path) { FolderScanState.Ready }
    }

    private fun markError(path: Path, msg: String) {
        updateState(path) { FolderScanState.Error(msg) }
    }

    private fun updateState(
        path: Path,
        state: () -> FolderScanState
    ) {
        _folders.value = _folders.value.map {
            if (it.path == path) it.copy(state = state())
            else it
        }
    }

    fun refreshAllOnStartup() {
        _folders.value.forEach {
            enqueueScan(it.path)
        }
    }

}
