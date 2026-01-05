package org.example


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.audioindex.AudioFolderController
import org.example.bass.Bass
import org.example.folderGetter.FolderScanController
import org.example.ui.screens.leftPager.settings.AppPrefs
import ui.draw
import win32helpers.WinFullscreen
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.KeyStroke
import kotlin.io.path.Path

// глобальное состояние
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


fun main() {

    val bass = Bass.INSTANCE

    val ok = bass.BASS_Init(
        -1,        // default audio device
        44100,     // sample rate
        0,
        0,
        0
    )

    if (!ok) {
        error("BASS_Init failed, error=${bass.BASS_ErrorGetCode()}")
    }

    val stream = bass.BASS_StreamCreateFile(
        false,
        "C:\\Users\\sasha\\Downloads\\Born Under Punches (The Heat Goes On) - 2005 Remaster - Talking Heads.mp3",
        0,
        0,
        0
    )

    if (stream == 0) {
        error("Stream create failed, error=${bass.BASS_ErrorGetCode()}")
    }

    bass.BASS_ChannelPlay(stream, false)
    

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
            WinFullscreen.enter(frame)
            prefs.putBoolean(PREF_FULLSCREEN, true)
        }

        fun exitFullscreen() {
            val restoreBounds = previousBounds ?: savedBounds
            WinFullscreen.exit(frame, restoreBounds)
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

    val audioFolderController = remember {
        AudioFolderController()
    }

    val folderScanController = remember {
        FolderScanController(audioFolderController)
    }

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

    var isReady by remember { mutableStateOf(false) }

    // Основной UI
    CompositionLocalProvider(
        LocalDensity provides Density(
            loaderConfig.dpiScale.value,
            loaderConfig.dpiScale.value
        )
    ) {

        if (!isReady) {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().background(Color(20, 20, 20))){
                CircularProgressIndicator(color = Color(255, 255, 255))
            }

            LaunchedEffect(Unit)
            {
                withContext(Dispatchers.IO) {
                    audioFolderController.start(Path("folders.config"))
                    folderScanController.restoreFromAudioController()

                    if (shouldUpdateOnStart)
                        folderScanController.refreshAllOnStartup()
                }
                isReady = true
            }
        }
        else
            draw(fullscreen, audioFolderController, folderScanController )

    }

}

