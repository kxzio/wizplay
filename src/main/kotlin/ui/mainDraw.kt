package ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.forEach
import org.example.folderGetter.FolderScanController
import org.example.FullscreenController
import org.example.audioindex.AudioFolderController
import org.example.bassAudioController
import org.example.loaderConfig
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.wizui.wizui.FlatSliderTrack
import ui.screens.leftPager.renderLeftPager
import ui.screens.rightPager.formatTime
import ui.screens.rightPager.renderRightPager
import ui.uiHelpers.myTypography

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun draw(
    fullscreen: FullscreenController,
    audioFolderController: AudioFolderController,
    folderScanController: FolderScanController
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

        val openedAudioSource = remember {
            mutableStateOf(AppPrefs.getString("openedAudioSource", ""))
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {

            val maxWidth = this.maxWidth

            val hazeState = rememberHazeState()

            Row(Modifier.fillMaxSize().hazeSource(hazeState))
            {
                renderLeftPager(openedAudioSource, maxWidth, fullscreen, audioFolderController, folderScanController)

                renderRightPager( audioFolderController, openedAudioSource)

            }

        }
    }


}