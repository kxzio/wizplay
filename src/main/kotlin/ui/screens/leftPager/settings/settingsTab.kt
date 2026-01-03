package ui.screens.leftPager.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBackIos
import androidx.compose.material.icons.automirrored.sharp.ArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.FullscreenController
import org.example.loaderConfig
import org.example.wizui.toHexString
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun settingTab(
    openedTab: MutableState<Int>,
    openedSettingsTab: MutableState<Int>,
    fullscreen: FullscreenController
)
{
    (openedTab.value == 1).wizAnimateIf(wizui.WizAnimationType.ExpandVertically) {
        Column(Modifier.padding(top = 8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

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

                        var listoftabs = listOf("", "folders", "interface", "audio")
                        var tab = listoftabs[openedSettingsTab.value]
                        Text(" / $tab ",
                            color = Color(255, 255, 255, 150),
                            fontSize = 22.sp,
                        )
                    }
                }
            }


            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                thickness = 1.0.dp,
                color = MaterialTheme.colorScheme.primary
            )

            AnimatedContent(
                targetState = openedSettingsTab.value,
                transitionSpec = {
                    val direction =
                        if (targetState > initialState)
                            AnimatedContentTransitionScope.SlideDirection.Left
                        else
                            AnimatedContentTransitionScope.SlideDirection.Right

                    slideIntoContainer(
                        towards = direction,
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    ) + fadeIn() togetherWith
                            slideOutOfContainer(
                                towards = direction,
                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                            ) + fadeOut()
                },
                label = "settingsTransition"
            ) { tab ->

                when (tab) {
                    0 -> {

                        Column {
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
                                    Text("folders")
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
                                    Text("audio")
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.AutoMirrored.Sharp.ArrowRight, "")
                                }
                            }
                        }

                    }

                    1 -> {

                        Column {
                            Row {
                                Text(
                                    "select folders to scan",
                                    fontSize = 12.sp,
                                    color = Color(255, 255, 255, 100)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            if (loaderConfig.foldersToScan.isEmpty()) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
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

                            wizui.wizVerticalList(
                                modifier = Modifier.fillMaxWidth().height(150.dp)
                                    .background(Color(20, 20, 20)),
                                items = loaderConfig.foldersToScan,
                            ) { item ->
                                Column() {
                                    Text(item)
                                }
                            }
                        }
                    }

                    2 -> {

                        val scrollState = rememberScrollState()
                        Column(Modifier.verticalScroll(scrollState)) {

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

                        }

                    }
                }

            }



        }
    }
}