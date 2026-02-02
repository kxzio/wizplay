package org.example.ui.screens.leftPager.settings.settingPages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Devices
import androidx.compose.material.icons.sharp.DevicesOther
import androidx.compose.material.icons.sharp.Headset
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material.icons.sharp.SpatialAudio
import androidx.compose.material.icons.sharp.Stream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.example.PREF_AUDIOOUTPUT
import org.example.bass.bassController.serializeAudioOutput
import org.example.bassAudioController
import org.example.folderGetter.FolderScanState
import org.example.prefs
import org.example.wizui.wizui

@Composable
fun drawSoundSettings()
{
    val devices = remember { bassAudioController.getAudioDevices() }

    Column(modifier = Modifier.fillMaxSize()) {

        Text("${devices.size} audio outputs : ", color = Color(255, 255, 255), fontSize = 18.sp)

        Spacer(Modifier.height(20.dp))

        wizui.wizVerticalList(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color(25, 25, 25)),
            items = devices
        ) { item ->

            wizui.wizButton(
                delayedClick = true,
                delayedClickDurationMs = 300,
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth(),
                contentColor = Color(255, 255, 255),
                backgroundColor = Color(35, 35, 35),
                onClick = {
                    bassAudioController.switchAudioDevice(item.id)
                    prefs.put(PREF_AUDIOOUTPUT, serializeAudioOutput(item))

                }
            ) {

                val currentDevice by bassAudioController.state
                    .map { it.currentDevice }
                    .distinctUntilChanged()
                    .collectAsState(initial = -1)

                Row(Modifier.align(Alignment.Start).fillMaxWidth().alpha(
                    if (currentDevice == item.id) 1f else 0.3f
                ), verticalAlignment = Alignment.CenterVertically) {

                    Icon((Icons.Sharp.Headset), null, tint = Color.White)

                    Spacer(Modifier.width(20.dp))

                    Text(
                        if (item.isDefault)
                            "${item.name} (default)"
                        else item.name
                    )
                }


            }
        }
    }

}