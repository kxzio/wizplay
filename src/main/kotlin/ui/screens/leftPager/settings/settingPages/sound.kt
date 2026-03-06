package org.example.ui.screens.leftPager.settings.settingPages

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.sharp.Headset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.coreMaster.grooviqCore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import core.preferencesAndToolsForCore.PREF_AUDIOOUTPUT
import core.preferencesAndToolsForCore.PREF_AUDIO_VOLUME
import core.preferencesAndToolsForCore.PREF_REPLAY_GAIN
import org.example.bass.bassController.serializeAudioOutput
import core.preferencesAndToolsForCore.prefs
import org.example.wizui.wizui
import kotlin.math.roundToInt

@Composable
fun drawSoundOutputsSettings()
{
    val devices = remember { grooviqCore.controllers.audioController.bassAudioController.getAudioDevices() }

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
                    grooviqCore.controllers.audioController.bassAudioController.switchAudioDevice(item.id)
                    prefs.put(PREF_AUDIOOUTPUT, serializeAudioOutput(item))

                }
            ) {

                val currentDevice by grooviqCore.controllers.audioController.bassAudioController.state
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


@Composable
fun drawSoundSettings()
{

    Column(modifier = Modifier.fillMaxSize()) {

        var volume by remember {
            mutableStateOf(prefs.getFloat(PREF_AUDIO_VOLUME, 1f))
        }

        Text("basic volume ( ${(volume * 100f).roundToInt()}% ) : ", color = Color(255, 255, 255))

        Spacer(Modifier.height(26.dp))

        val sliderColors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            activeTickColor = Color(28, 28, 28),
            inactiveTickColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color(28, 28, 28)
        )


        wizui.wizSlider(
            value = volume,
            onValueChange = {
                volume = it
                prefs.putFloat(PREF_AUDIO_VOLUME, it)
                grooviqCore.controllers.audioController.bassAudioController.setVolume(it)
            },
            valueRange = 0f..1.0f,
            steps = 9,
            sliderColors = sliderColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            thickness = 1.0.dp,
            color = Color(60, 60, 60)
        )

        var replayGain by remember {
            mutableStateOf(prefs.getBoolean(PREF_REPLAY_GAIN, true))
        }

        wizui.wizCheckBox(
            text = "static volume normalization (replay gain)",
            checked = replayGain,
            onCheckedChange = { checked ->
                replayGain = checked
                grooviqCore.controllers.audioController.bassAudioController.setReplayGainEnabled(checked)
                prefs.putBoolean(PREF_REPLAY_GAIN, checked)
            }
        )
        Text("normalization volume between albums and different tracks, also known as replay gain",
            modifier = Modifier.padding(top = 4.dp),
            color = Color(255, 255, 255, 100),
            fontSize = 11.sp
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            thickness = 1.0.dp,
            color = Color(60, 60, 60)
        )



    }

}