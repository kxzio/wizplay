package org.example


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.coreMaster.grooviqCore
import core.preferencesAndToolsForCore.PREF_AUDIOOUTPUT
import core.preferencesAndToolsForCore.PREF_AUDIO_VOLUME
import core.preferencesAndToolsForCore.PREF_FULLSCREEN
import core.preferencesAndToolsForCore.PREF_NORMALIZATION
import core.preferencesAndToolsForCore.PREF_REPLAY_GAIN
import core.preferencesAndToolsForCore.loadWindowBounds
import core.preferencesAndToolsForCore.prefs
import core.preferencesAndToolsForCore.saveWindowBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.example.audioindex.AudioFolderController
import org.example.bass.bassController.deserializeAudioOutput
import org.example.folderGetter.FolderScanController
import org.example.folderGetter.PlaylistController
import org.example.ui.screens.leftPager.settings.AppPrefs
import ui.draw
import unixSystemSpecificCode.CrossPlatformFullscreen
import unixSystemSpecificCode.WinFullscreen
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.KeyStroke


var loaderConfig by mutableStateOf(LocalConfig())

fun saveConfigOnExit() {
    val configToSave = loaderConfig.toConfig()
    writeConfig("config.data", configToSave)
}


class FullscreenController(
    private val enter: () -> Unit,
    private val exit: () -> Unit
) {
    var isFullscreen by mutableStateOf(false)
        private set

    fun enterFullscreen() {
        if (isFullscreen) return
        enter()
        isFullscreen = true
    }

    fun exitFullscreen() {
        if (!isFullscreen) return
        exit()
        isFullscreen = false
    }

    fun toggle() {
        if (isFullscreen) exitFullscreen()
        else enterFullscreen()
    }
}

val LocalFullscreenController =
    staticCompositionLocalOf<FullscreenController> {
        error("FullscreenController not provided")
    }

//val bassQueueController = QueueController()
//val bassAudioController = PlayerController()

fun main() {

    loaderConfig.apply(readConfig("config.data"))

    EventQueue.invokeLater {
        val frame = JFrame("wizplay").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        }

        val savedBounds = loadWindowBounds()
        frame.bounds = savedBounds
        if (savedBounds.x <= 0 && savedBounds.y <= 0) {
            frame.setLocationRelativeTo(null)
        }

        var previousBounds: Rectangle? = null
        val wasFullscreen = prefs.getBoolean(PREF_FULLSCREEN, false)

        fun enterFullscreen() {
            previousBounds = frame.bounds
            frame.isVisible = true

            if (OS.isWindows) {
                WinFullscreen.enter(frame)
            } else {
                CrossPlatformFullscreen.enter(frame)
            }

            prefs.putBoolean(PREF_FULLSCREEN, true)
        }

        fun exitFullscreen() {
            val restoreBounds = previousBounds ?: savedBounds

            if (OS.isWindows) {
                WinFullscreen.exit(frame, restoreBounds)
            } else {
                CrossPlatformFullscreen.exit(frame)
            }

            frame.isVisible = true
            saveWindowBounds(frame.bounds)

            prefs.putBoolean(PREF_FULLSCREEN, false)
        }

        val fullscreenController = FullscreenController(
            enter = ::enterFullscreen,
            exit = ::exitFullscreen
        )

        val composePanel = ComposePanel().apply {
            isFocusable = true
            setContent {
                CompositionLocalProvider(
                    LocalFullscreenController provides fullscreenController
                ) {
                    preDraw()
                }
            }
        }

        frame.add(composePanel)

        val im = composePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val am = composePanel.actionMap

        im.put(KeyStroke.getKeyStroke("F11"), "toggle")
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "exit")

        am.put("toggle", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                fullscreenController.toggle()
            }
        })

        am.put("exit", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                fullscreenController.exitFullscreen()
            }
        })

        frame.isVisible = true
        composePanel.requestFocusInWindow()

        if (wasFullscreen) {
            enterFullscreen()
        }

        frame.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                if (!fullscreenController.isFullscreen) {
                    saveWindowBounds(frame.bounds)
                }
            }
        })
    }
}

@Composable
fun preDraw() {


    val fullscreen = LocalFullscreenController.current

    DisposableEffect(Unit) {
        val hook = Thread {
            saveConfigOnExit()
        }

        Runtime.getRuntime().addShutdownHook(hook)

        onDispose {

        }
    }

    val shouldUpdateOnStart = AppPrefs.getBool("shouldUpdate", false)

    val isReady by grooviqCore.controllers.isLoadingReady.collectAsState()

    val openedAudioSource = remember {
        mutableStateOf(AppPrefs.getString("openedAudioSource", ""))
    }

    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        grooviqCore.controllers.dataController.shouldUpdateFoldersOnStart = shouldUpdateOnStart
        grooviqCore.controllers.init()
    }

    if (!isReady) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(20, 20, 20))
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.weight(1f))

                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    color = Color(255, 255, 255),
                    modifier = Modifier.size(120.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    "initialization",
                    color = Color(255, 255, 255, 255),
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(14.dp))

                val logs by grooviqCore.controllers.initLogs.collectAsState()

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    items(logs) {
                        Text(
                            it,
                            modifier = Modifier.animateItem(),
                            color = Color(255, 255, 255, 120),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        }

    }
    else
    {
        // Основной UI
        CompositionLocalProvider(
            LocalDensity provides Density(
                loaderConfig.dpiScale.value,
                loaderConfig.dpiScale.value
            )
        ) {

            draw(
                fullscreen =  fullscreen,
                audioFolderController = grooviqCore.controllers.dataController.audioFolderController,
                folderScanController  = grooviqCore.controllers.dataController.folderScanController,
                playlistController    = grooviqCore.controllers.dataController.playlistController,
                openedAudioSource
            )

            //return@CompositionLocalProvider

            val fps by FpsCounter()

            Text(
                text = "FPS: $fps",
                color = Color.Green,
                modifier = Modifier.padding(8.dp)
            )

        }
    }

}

