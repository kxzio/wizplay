package ui.screens.leftPager

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.folderGetter.FolderScanController
import org.example.FullscreenController
import org.example.audioindex.AudioFolderController
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.wizui.wizui
import ui.screens.leftPager.albums.albumTab
import ui.screens.leftPager.settings.settingTab

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun renderLeftPager(
    openedAudioSource: MutableState<String>,
    maxWidth: Dp,
    fullscreen: FullscreenController,
    audioFolderController: AudioFolderController,
    folderScanController: FolderScanController
)
{

    val allowResize = remember {
        mutableStateOf(AppPrefs.getBool("allowResize", false))
    }

    wizui.wizColumn(
        resizable = allowResize.value,
        parentMaxWidth = maxWidth,

        initialWidthFraction = 0.5f,
        minWidthFraction = 0.2f,
        maxWidthFraction = 0.6f,

        modifier = Modifier
            .background(Color(20, 20, 20))
            .then(
                if (!allowResize.value)
                    Modifier.fillMaxWidth(0.5f)
                else
                    Modifier
            )
    ) {
        leftPagerContent(
            openedAudioSource,
            allowResize = allowResize,
            maxWidth = maxWidth,
            fullscreen = fullscreen,
            audioFolderController = audioFolderController,
            folderScanController = folderScanController
        )
    }


}

@Composable
fun leftPagerContent(
    openedAudioSource: MutableState<String>,
    allowResize: MutableState<Boolean>,
    maxWidth: Dp,
    fullscreen: FullscreenController,
    audioFolderController: AudioFolderController,
    folderScanController: FolderScanController
)
{
    val openedTab = remember { mutableStateOf(1) }
    var openedSettingsTab = remember { mutableStateOf(0) }

    val gridMultiplier = remember {
        mutableStateOf(AppPrefs.getFloat("gridMultiplier", 0f))
    }

    Column(Modifier.padding()) {

        Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 16.dp))
        {
            wizui.wizButton(
                contentColor = Color(255, 255, 255, 100),
                contentColorToggled = MaterialTheme.colorScheme.primary,
                backgroundColor = Color(255, 255, 255, 5),
                turnOffToggleIndication = true,
                modifier = Modifier.height(50.dp).border(1.dp, Color(255, 255, 255, 30)),
                shape = RectangleShape,
                onClick = {
                    openedTab.value = 1
                },
                toggleVariable = openedTab.value == 1
            )
            {
                Icon(Icons.Sharp.Settings, "")
            }

            wizui.wizButton(
                contentColor = Color(255, 255, 255, 100),
                contentColorToggled = MaterialTheme.colorScheme.primary,
                backgroundColor = Color(255, 255, 255, 5),
                turnOffToggleIndication = true,
                modifier = Modifier.weight(1f).height(50.dp).border(1.dp, Color(255, 255, 255, 30)),
                shape = RectangleShape,
                onClick = {
                    openedTab.value = 2
                },
                toggleVariable = openedTab.value == 2
            ) {
                Text("albums", fontSize = 16.sp,)
            }

            wizui.wizButton(
                contentColor = Color(255, 255, 255, 100),
                contentColorToggled = MaterialTheme.colorScheme.primary,
                backgroundColor = Color(255, 255, 255, 10),
                turnOffToggleIndication = true,
                modifier = Modifier.weight(1f).height(50.dp).border(1.dp, Color(255, 255, 255, 30)),
                shape = RectangleShape,
                onClick = {
                    openedTab.value = 3
                },
                toggleVariable = openedTab.value == 3
            )
            {
                Text("playlists", fontSize = 16.sp,)
            }
        }

        val pagerState = rememberPagerState(
            initialPage = openedTab.value - 1,
            pageCount = { 2 }
        )

        LaunchedEffect(openedTab.value) {
            pagerState.animateScrollToPage(openedTab.value - 1)
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondViewportPageCount = 0,
            modifier = Modifier.fillMaxSize()
        ) { page ->

            when (page) {
                0 -> settingTab(
                    allowResize,
                    openedTab,
                    openedSettingsTab,
                    fullscreen,
                    audioFolderController,
                    gridMultiplier,
                    folderScanController
                )

                1 -> albumTab(
                    audioFolderController,
                    openedTab,
                    gridMultiplier,
                    openedAudioSource
                )
            }
        }






    }
}