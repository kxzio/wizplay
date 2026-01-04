package ui.screens.leftPager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.AudioFolderController
import org.example.FolderScanController
import org.example.FullscreenController
import org.example.wizui.wizui
import ui.screens.leftPager.albums.albumTab
import ui.screens.leftPager.settings.settingTab

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun renderLeftPager(
    fullscreen: FullscreenController,
    audioFolderController: AudioFolderController,
    folderScanController: FolderScanController
)
{
    wizui.wizColumn(modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.5f)
        .background(Color(20, 20, 20))
    )
    {
        val openedTab = remember { mutableStateOf(1) }
        var openedSettingsTab = remember { mutableStateOf(0) }

        Column(Modifier.padding(16.dp)) {

            Row(Modifier.fillMaxWidth())
            {
                wizui.wizButton(
                    contentColor = Color(255, 255, 255),
                    contentColorToggled = MaterialTheme.colorScheme.primary,
                    turnOffToggleIndication = true,
                    modifier = Modifier.height(50.dp),
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
                    contentColor = Color(255, 255, 255),
                    contentColorToggled = MaterialTheme.colorScheme.primary,
                    turnOffToggleIndication = true,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RectangleShape,
                    onClick = {
                        openedTab.value = 2
                    },
                    toggleVariable = openedTab.value == 2
                )
                {
                    Text("albums", fontSize = 16.sp,)
                }

                wizui.wizButton(
                    contentColor = Color(255, 255, 255),
                    contentColorToggled = MaterialTheme.colorScheme.primary,
                    turnOffToggleIndication = true,
                    modifier = Modifier.weight(1f).height(50.dp),
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

            Spacer(Modifier.height(8.dp))

            //render of setting tab.    OPENED TAB = 1
            settingTab(openedTab, openedSettingsTab, fullscreen, audioFolderController, folderScanController)

            //render of album tab.      OPENED TAB = 2
            albumTab(openedTab)


        }

    }
}