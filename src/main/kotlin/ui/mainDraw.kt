package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.example.folderGetter.FolderScanController
import org.example.FullscreenController
import org.example.audioindex.AudioFolderController
import org.example.bassAudioController
import org.example.folderGetter.PlaylistController
import org.example.loaderConfig
import org.example.ui.screens.trackFullScreen
import ui.screens.leftPager.renderLeftPager
import ui.screens.rightPager.renderRightPager
import ui.uiHelpers.myTypography

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
fun draw(
    fullscreen: FullscreenController,
    audioFolderController: AudioFolderController,
    folderScanController: FolderScanController,
    playlistController: PlaylistController,
    openedAudioSource: MutableState<String>
)  {

    val colors = darkColorScheme(
        primary = Color(0xFF4CAF50),
        background = Color(0xFF1E1E1E),
        surface = Color(0xFF2A2A2A),
        onBackground = Color.White,
        onSurface = Color.White
    )

    val primary = loaderConfig.themeColor.value

    val colorScheme = colors.copy(
        primary = primary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = myTypography
    ) {

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    if (event.keyboardModifiers.isCtrlPressed) {
                        val delta = event.changes.first().scrollDelta.y

                        val sensitivity = 0.05f

                        loaderConfig.dpiScale.value = (loaderConfig.dpiScale.value - delta * sensitivity)
                            .coerceIn(0.1f, 10f)
                    }
                }
        ) {

            val maxWidth = this.maxWidth

            val hazeState = rememberHazeState()

            val state by bassAudioController.state.collectAsState()

            val stateHolder = rememberSaveableStateHolder()

            val overlayEnabled = remember { mutableStateOf(false) }


            Box(Modifier.fillMaxSize().background(Color(20, 20, 20))) {

                AnimatedVisibility(
                    visible = !overlayEnabled.value,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    stateHolder.SaveableStateProvider("MAIN") {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState)
                        ) {
                            renderLeftPager(
                                openedAudioSource,
                                maxWidth,
                                fullscreen,
                                audioFolderController,
                                folderScanController,
                                playlistController
                            )

                            renderRightPager(
                                audioFolderController,
                                openedAudioSource,
                                playlistController,
                                overlayEnabled
                            )

                        }
                    }
                }

                AnimatedVisibility(
                    visible = overlayEnabled.value,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    stateHolder.SaveableStateProvider("OVERLAY") {
                        trackFullScreen(overlayEnabled)
                    }
                }
            }


        }

    }


}