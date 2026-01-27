package org.example.ui.screens.leftPager.settings.settingPages

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.FullscreenController
import org.example.loaderConfig
import org.example.ui.screens.leftPager.settings.AppPrefs
import org.example.wizui.toHexString
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf

@Composable
fun interfaceSettings(fullscreen: FullscreenController, allowResize: MutableState<Boolean>)
{
    val scrollState = rememberSaveable(
        saver = ScrollState.Saver
    ) {
        ScrollState(0)
    }

    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {

        Spacer(Modifier.height(2.dp))

        Text(
            "dpi scale",
            color = Color(255, 255, 255, 255)
        )

        Spacer(Modifier.height(26.dp))

        val sliderColors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            activeTickColor = Color(28, 28, 28),
            inactiveTickColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color(28, 28, 28)
        )

        wizui.wizSlider(
            value = loaderConfig.dpiScale.value,
            onValueChange = {
                loaderConfig.dpiScale.value = it
            },
            valueRange = 1f..1.5f,
            steps = 10,
            sliderColors = sliderColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            thickness = 1.0.dp,
            color = Color(60, 60, 60)
        )


        Column {

            Text(
                "display-mode",
                color = Color(255, 255, 255, 255)
            )


            Spacer(Modifier.height(12.dp))

            Column(Modifier.padding(start = 16.dp)) {
                wizui.wizRadioButton(
                    "floating window",
                    selected = !fullscreen.isFullscreen,
                    backgroundColor = Color(70, 70, 70, 255),
                    onSelect = {
                        fullscreen.exitFullscreen()
                    }
                )

                wizui.wizRadioButton(
                    "fullscreen",
                    selected = fullscreen.isFullscreen,
                    backgroundColor = Color(70, 70, 70, 255),
                    onSelect = {
                        fullscreen.enterFullscreen()
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                thickness = 1.0.dp,
                color = Color(60, 60, 60)
            )
        }


        wizui.wizCheckBox(
            text = "allow resizable layout",
            checked = allowResize.value,
            onCheckedChange = { checked ->
                allowResize.value = checked
                AppPrefs.setBool("allowResize", checked)
            }
        )
        Text("grab layout on it's edge to change size",
            modifier = Modifier.padding(top = 4.dp),
            color = Color(255, 255, 255, 100),
            fontSize = 11.sp
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            thickness = 1.0.dp,
            color = Color(60, 60, 60)
        )


        Text(
            "primary color",
            color = Color(255, 255, 255, 255)
        )

        Spacer(Modifier.height(16.dp))

        wizui.wizColorPicker(
            initialColor = loaderConfig.themeColor.value,
            onColorChanged = { loaderConfig.themeColor.value = it },
            height = 200.dp
        )

        Row(Modifier.padding(top = 12.dp)) {

            (loaderConfig.themeColor.value != Color(0xFF4CAF50)).
            wizAnimateIf(wizui.WizAnimationType.ExpandHorizontally) {
                wizui.wizButton(
                    shape = RectangleShape,
                    contentColor = Color(255, 255, 255),
                    backgroundColor = Color(35, 35, 35),
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    onClick = {
                        loaderConfig.themeColor.value = Color(0xFF4CAF50)
                    }
                ){
                    Text("reset", fontSize = 14.sp)
                }
            }

            wizui.wizButton(
                shape = RectangleShape,
                contentColor = Color(255, 255, 255),
                modifier = Modifier.fillMaxSize(),
                onClick = {}
            ){
                Text(loaderConfig.themeColor.value.toHexString(),  fontSize = 14.sp)
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            thickness = 1.0.dp,
            color = Color(60, 60, 60)
        )



    }

}