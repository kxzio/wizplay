package core.coreMaster

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import core.preferencesAndToolsForCore.PREF_AUDIOOUTPUT
import core.preferencesAndToolsForCore.PREF_AUDIO_VOLUME
import core.preferencesAndToolsForCore.PREF_NORMALIZATION
import core.preferencesAndToolsForCore.PREF_REPLAY_GAIN
import org.example.bass.bassController.PlayerController
import org.example.bass.queue.QueueController
import core.preferencesAndToolsForCore.prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.LocalConfig
import org.example.audioindex.AudioFolderController
import org.example.bass.bassController.deserializeAudioOutput
import org.example.folderGetter.FolderScanController
import org.example.folderGetter.PlaylistController
import org.example.writeConfig

//coroutines
val coroutineForLoading =
    CoroutineScope(SupervisorJob() + Dispatchers.IO)

val coroutineForInit =
    CoroutineScope(SupervisorJob() + Dispatchers.IO)

object grooviqCore {

    object controllers {

        var isLoadingReady = MutableStateFlow(false)

        var audioInitReady = MutableStateFlow(false)
        var dataInitReady  = MutableStateFlow(false)

        val initLogs = MutableStateFlow<List<String>>(emptyList())

        object audioController {

            //create queue controller
            lateinit var bassQueueController : QueueController

            //create bass controller
            lateinit var bassAudioController : PlayerController

            fun init() {

                coroutineForInit.launch {

                    initLogs.value += ("player / queue controller create")
                    bassAudioController = PlayerController()
                    bassQueueController = QueueController()

                    val audioOutputConfig           : String    = prefs.get(PREF_AUDIOOUTPUT, "")
                    val audioVolumeConfig           : Float     = prefs.getFloat(PREF_AUDIO_VOLUME, 1f)
                    val audioReplayGainConfig       : Boolean   = prefs.getBoolean(PREF_REPLAY_GAIN, true)
                    val audioNormalizationConfig    : Boolean   = prefs.getBoolean(PREF_NORMALIZATION, true)

                    initLogs.value += ("bass init")
                    //using audio outputs on loading
                    bassAudioController.init(
                        deserializeAudioOutput(audioOutputConfig)
                    )

                    bassQueueController.attachPlayer(bassAudioController)

                    //setting the last saved volume
                    bassAudioController.setVolume(audioVolumeConfig)
                    bassAudioController.
                    _state.update {
                        //process the last saved replay gain on start
                        it.copy(
                            replayGainEnabled = audioReplayGainConfig,
                        )
                    }

                    audioInitReady.value = true

                }

            }
        }

        object dataController {

            var shouldUpdateFoldersOnStart  : Boolean           = false

            lateinit var audioFolderController      : AudioFolderController
            lateinit var playlistController         : PlaylistController
            lateinit var folderScanController       : FolderScanController

            fun init() {

                isLoadingReady.value = false

                coroutineForLoading.launch {

                    println(Thread.currentThread().name)

                    initLogs.value += ("folder controller init")
                    audioFolderController   = AudioFolderController()
                    playlistController      = PlaylistController(audioFolderController.db)
                    folderScanController    = FolderScanController(audioFolderController)

                    //load database / albums
                    initLogs.value += ("loading albums")
                    audioFolderController.start()

                    //restore folder controller from database
                    folderScanController.restoreFromAudioController()

                    //load playlists
                    initLogs.value += ("loading playlists")
                    playlistController.load()

                    withContext(Dispatchers.Main) {
                        dataInitReady.value = true
                    }

                    if (shouldUpdateFoldersOnStart)
                        folderScanController.refreshAllOnStartup()


                }


            }
        }

        fun init() {

            try {

                dataController  .init()
                audioController .init()

                coroutineForInit.launch {

                    audioInitReady.first { it }
                    dataInitReady .first { it }

                    isLoadingReady.value = true

                }

            }
            catch (e : Exception) {
                initLogs.value += (e.message ?: e.toString())
            }

        }

    }

    init {

    }

}



