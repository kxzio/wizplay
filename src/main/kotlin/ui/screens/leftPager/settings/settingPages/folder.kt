package org.example.ui.screens.leftPager.settings.settingPages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.folderGetter.FolderScanController
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.ui.screens.leftPager.settings.folderScanContent
import org.example.wizui.wizui

@Composable
fun folderSettings(folderScanController : FolderScanController)
{
    Column(Modifier.fillMaxSize()) {

        val shouldUpdateOnStart = remember {
            mutableStateOf(AppPrefs.getBool("shouldUpdate", false))
        }

        wizui.wizCheckBox(
            text = "update folders on application open",
            checked = shouldUpdateOnStart.value,
            onCheckedChange = { checked ->
                shouldUpdateOnStart.value = checked
                AppPrefs.setBool("shouldUpdate", checked)
            }
        )

        Spacer(Modifier.height(16.dp))

        Row {
            Text(
                "select folders to scan",
                fontSize = 12.sp,
                color = Color(255, 255, 255, 100)
            )
        }

        Spacer(Modifier.height(8.dp))

        val folders by folderScanController.folders.collectAsState()

        if (folders.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(30, 30, 30)),
                contentAlignment = Alignment.Center
            )
            {
                Text(
                    "folders empty..",
                    fontSize = 12.sp,
                    color = Color(255, 255, 255)
                )
            }
        }

        folderScanContent(
            folderScanController,
            folders = folders,
            onAddFolder = { folderScanController.addFolder(it) },
            onRemoveFolder = { folderScanController.removeFolder(it) }
        )


    }
}