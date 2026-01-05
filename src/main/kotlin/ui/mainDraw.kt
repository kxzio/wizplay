package ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.example.folderGetter.FolderScanController
import org.example.FullscreenController
import org.example.audioindex.AudioFolderController
import org.example.loaderConfig
import org.example.ui.screens.leftPager.settings.AppPrefs
import ui.screens.leftPager.renderLeftPager
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

            Row(Modifier.fillMaxSize())
            {
                renderLeftPager(openedAudioSource, maxWidth, fullscreen, audioFolderController, folderScanController)

                renderRightPager( audioFolderController, openedAudioSource)

            }
        }
    }


}