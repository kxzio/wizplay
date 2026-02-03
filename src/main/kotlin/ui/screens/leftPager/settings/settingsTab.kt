package ui.screens.leftPager.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBackIos
import androidx.compose.material.icons.automirrored.sharp.ArrowRight
import androidx.compose.material.icons.sharp.Audiotrack
import androidx.compose.material.icons.sharp.Draw
import androidx.compose.material.icons.sharp.SettingsPower
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.folderGetter.FolderScanController
import org.example.FullscreenController
import org.example.audioindex.AudioFolderController
import org.example.ui.screens.leftPager.settings.settingPages.drawSoundOutputsSettings
import org.example.ui.screens.leftPager.settings.settingPages.drawSoundSettings
import org.example.ui.screens.leftPager.settings.settingPages.folderSettings
import org.example.ui.screens.leftPager.settings.settingPages.interfaceSettings
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun settingTab(
    allowResize: MutableState<Boolean>,
    openedTab: MutableState<Int>,
    openedSettingsTab: MutableState<Int>,
    fullscreen: FullscreenController,
    audioFolderController: AudioFolderController,
    gridMultiplier: MutableState<Float>,
    folderScanController: FolderScanController
)
{

    Column(Modifier.fillMaxSize()) {

        Row(modifier = Modifier.padding(start = 32.dp, end = 32.dp),
            verticalAlignment = Alignment.CenterVertically) {

            Box(modifier = Modifier.offset(y = 1.dp).onClick {
                openedSettingsTab.value = 0
            }) {
                (openedSettingsTab.value != 0).wizAnimateIf(
                    speedIn = 250,
                    speedOut = 250,
                    type = wizui.WizAnimationType.ExpandHorizontally
                ) {

                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.AutoMirrored.Sharp.ArrowBackIos,
                        contentDescription = "",
                        tint = Color(255, 255, 255),
                    )

                }
            }

            Row {

                wizui.wizBlinkingText(
                    "settings",
                    normalColor = Color(255, 255, 255),
                    blinkColor = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 12.dp),
                    onClick = {
                        openedSettingsTab.value = 0
                    }
                )

                (openedSettingsTab.value != 0).wizAnimateIf {

                    var listoftabs = listOf("", "folders", "interface", "outputs", "sound")
                    var tab = listoftabs[openedSettingsTab.value]
                    Text(" / $tab ",
                        color = Color(255, 255, 255, 150),
                        fontSize = 22.sp,
                    )
                }
            }
        }


        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 32.dp),
            thickness = 1.0.dp,
            color = MaterialTheme.colorScheme.primary
        )

        val pagerState = rememberPagerState(
            initialPage = if (openedSettingsTab.value == 0) 0 else 1,
            pageCount = { 2 }
        )

        LaunchedEffect(openedSettingsTab.value) {
            pagerState.animateScrollToPage(
                if (openedSettingsTab.value == 0) 0 else 1
            )
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondViewportPageCount = 1,
            modifier = Modifier.padding(start = 32.dp, end = 32.dp)
        ) { page ->

            when (page) {

                0 -> {
                    Column(Modifier.fillMaxSize()) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                        ) {

                            Icon(Icons.Sharp.SettingsPower, "", tint = Color(255, 255, 255, 100))

                            Text("configuration", color = Color(255, 255, 255, 100), fontSize = 18.sp,
                                modifier = Modifier.padding(start = 16.dp))
                        }

                        wizui.wizButton(
                            delayedClick = true,
                            delayedClickDurationMs = 300,
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth(),
                            contentColor = Color(255, 255, 255),
                            backgroundColor = Color(35, 35, 35),
                            onClick = {
                                openedSettingsTab.value = 1
                            }
                        )
                        {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween
                            )
                            {
                                Text("scan folders")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Sharp.ArrowRight, "")
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Icon(Icons.Sharp.Draw, "", tint = Color(255, 255, 255, 100))

                            Text("ui", color = Color(255, 255, 255, 100), fontSize = 18.sp,
                                modifier = Modifier.padding(start = 16.dp))
                        }

                        wizui.wizButton(
                            delayedClick = true,
                            delayedClickDurationMs = 300,
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth(),
                            contentColor = Color(255, 255, 255),
                            backgroundColor = Color(35, 35, 35),
                            onClick = {
                                openedSettingsTab.value = 2
                            }
                        )
                        {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween
                            )
                            {
                                Text("interface")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Sharp.ArrowRight, "")
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Icon(Icons.Sharp.Audiotrack, "", tint = Color(255, 255, 255, 100))

                            Text("audio", color = Color(255, 255, 255, 100), fontSize = 18.sp,
                                modifier = Modifier.padding(start = 16.dp))
                        }

                        wizui.wizButton(
                            delayedClick = true,
                            delayedClickDurationMs = 300,
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth(),
                            contentColor = Color(255, 255, 255),
                            backgroundColor = Color(35, 35, 35),
                            onClick = {
                                openedSettingsTab.value = 4
                            }
                        )
                        {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween
                            )
                            {
                                Text("general audio settings")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Sharp.ArrowRight, "")
                            }
                        }

                        wizui.wizButton(
                            delayedClick = true,
                            delayedClickDurationMs = 300,
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth(),
                            contentColor = Color(255, 255, 255),
                            backgroundColor = Color(35, 35, 35),
                            onClick = {
                                openedSettingsTab.value = 3
                            }
                        )
                        {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween
                            )
                            {
                                Text("audio-outputs")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Sharp.ArrowRight, "")
                            }
                        }

                    }
                }

                1 ->
                {
                    if (openedSettingsTab.value == 1){
                        folderSettings(
                            folderScanController
                        )
                    }
                    else if (openedSettingsTab.value == 2)
                    {
                        interfaceSettings(
                            fullscreen,
                            allowResize
                        )
                    }
                    else if (openedSettingsTab.value == 3)
                    {
                        drawSoundOutputsSettings()
                    }
                    else if (openedSettingsTab.value == 4)
                    {
                        drawSoundSettings()
                    }
                }

            }
        }




    }

}