package org.example


import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import ui.draw
import win32helpers.WinFullscreen
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.KeyStroke

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


    val fullscreen = LocalFullscreenController.current

    DisposableEffect(Unit) {
        val hook = Thread {
            saveConfigOnExit()
        }

        Runtime.getRuntime().addShutdownHook(hook)

        onDispose {

        }
    }

    // Основной UI
    CompositionLocalProvider(
        LocalDensity provides Density(
            loaderConfig.dpiScale.value,
            loaderConfig.dpiScale.value
        )
    ) {
        draw(fullscreen)
    }

}

