package org.example

import Config
import LocalConfig
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBackIos
import androidx.compose.material.icons.automirrored.sharp.ArrowLeft
import androidx.compose.material.icons.automirrored.sharp.ArrowRight
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.ArrowLeft
import androidx.compose.material.icons.sharp.ArrowRight
import androidx.compose.material.icons.sharp.FolderSpecial
import androidx.compose.material.icons.sharp.Headphones
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.wizui.wizui

import androidx.compose.material3.Typography
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.example.wizui.wizui.wizAnimateIf
import readConfig
import writeConfig

// 🔑 Одна переменная для управления расстоянием между буквами
val letterSpacingValue = 1.3.sp

// Подключаем шрифты из ресурсов
val sfFontFamily = FontFamily(
    Font("fonts/SFUIDisplay-Light.ttf", weight = FontWeight.Normal),
    Font("fonts/SFUIDisplay-Light.ttf", weight = FontWeight.Bold)
)

// Полный набор Typography
val myTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        letterSpacing = letterSpacingValue
    ),
    displayMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        letterSpacing = letterSpacingValue
    ),
    displaySmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = letterSpacingValue
    ),
    headlineLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = letterSpacingValue
    ),
    headlineMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        letterSpacing = letterSpacingValue
    ),
    headlineSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = letterSpacingValue
    ),
    titleLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = letterSpacingValue
    ),
    titleMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = letterSpacingValue
    ),
    titleSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = letterSpacingValue
    ),
    bodyLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = letterSpacingValue
    ),
    bodyMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = letterSpacingValue
    ),
    bodySmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = letterSpacingValue
    ),
    labelLarge = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = letterSpacingValue
    ),
    labelMedium = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = letterSpacingValue
    ),
    labelSmall = TextStyle(
        fontFamily = sfFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = letterSpacingValue
    )
)


// глобальное состояние
var loaderConfig by mutableStateOf(LocalConfig())


val config = readConfig("config.data")


fun saveConfigOnExit(windowState: WindowState) {
    val configToSave = loaderConfig.toConfig().copy(
        windowSizeX = windowState.size.width.value.toInt(),
        windowSizeY = windowState.size.height.value.toInt()
    )
    writeConfig("config.data", configToSave)
}

fun main() = application {

    var isConfigLoaded by remember { mutableStateOf(false) }
    val windowState = rememberWindowState(
        width = config.windowSizeX.dp,
        height = config.windowSizeY.dp
    )

    DisposableEffect(Unit) {

        val hook = Thread {
            saveConfigOnExit(windowState)
        }

        Runtime.getRuntime().addShutdownHook(hook)

        onDispose {

        }
    }

    LaunchedEffect(Unit)
    {
        //load LoaderConfig
        loaderConfig.apply(readConfig("config.data"))
        isConfigLoaded = true
    }

    Window(
        onCloseRequest = {
            exitApplication()
        },
        state = windowState,
        title = "wizplay",
    ) {

        if (!isConfigLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(20, 20, 20)),
                contentAlignment = Alignment.Center
            ) {
            }
            return@Window
        }

        CompositionLocalProvider(
            LocalDensity provides Density(loaderConfig.dpiScale.value, loaderConfig.dpiScale.value)
        ) {
            draw()
        }
    }

}

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun draw()  {


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

        Row(Modifier.fillMaxSize())
        {
            wizui.wizColumn(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .background(Color(20, 20, 20))
            )
            {
                val openedTab = remember { mutableStateOf(1) }
                var openedSettingsTab by remember { mutableStateOf(0) }

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

                    var searchQr by remember { mutableStateOf("") }
                    var isFocused by remember { mutableStateOf(false) }

                    Spacer(Modifier.height(8.dp))

                    (openedTab.value != 1).wizAnimateIf(wizui.WizAnimationType.ExpandVertically) {
                        Column {
                            Row {
                                BasicTextField(
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    value = searchQr,
                                    onValueChange = { searchQr = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(35.dp)
                                        .border(
                                            width = 0.5.dp,
                                            color = if (searchQr.isNotEmpty() || isFocused)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            shape = RectangleShape
                                        )
                                        .onFocusChanged { focusState ->
                                            isFocused = focusState.isFocused
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    decorationBox = { innerTextField ->

                                        (searchQr.isEmpty() && !isFocused).wizAnimateIf {
                                            Text(
                                                "searching",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }

                                        innerTextField()
                                    }
                                )

                                wizui.wizButton({
                                    searchQr = ""
                                },
                                    modifier = Modifier.height(35.dp),
                                    shape = RectangleShape,
                                    contentColor = Color(255, 255, 255)
                                )
                                {
                                    Text("clear", fontSize = 14.sp)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text("results : ",
                                fontSize = 12.sp,
                                color = Color(255, 255, 255, 255))

                        }
                    }

                    (openedTab.value == 1).wizAnimateIf(wizui.WizAnimationType.ExpandVertically) {
                        Column(Modifier.padding(top = 8.dp)) {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Box(modifier = Modifier.offset(y = 1.dp).onClick {
                                    openedSettingsTab = 0
                                }) {
                                    (openedSettingsTab != 0).wizAnimateIf(
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
                                            openedSettingsTab = 0
                                        }
                                    )

                                    (openedSettingsTab != 0).wizAnimateIf {

                                        var listoftabs = listOf("", "folders", "interface", "audio")
                                        var tab = listoftabs[openedSettingsTab]
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
                                targetState = openedSettingsTab,
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
                                                    openedSettingsTab = 1
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
                                                    openedSettingsTab = 2
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
                                                    openedSettingsTab = 3
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

                                        Column {


                                            Text(
                                                "dpi scale",
                                                fontSize = 14.sp,
                                                color = Color(255, 255, 255, 255)
                                            )

                                            Spacer(Modifier.height(20.dp))

                                            val sliderColors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
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

                                            Spacer(Modifier.height(36.dp))

                                            Button(onClick = {
                                                loaderConfig.themeColor.value = Color(0xFF4CAF50) // default
                                            }) {
                                                Text("Default")
                                            }

                                            Button(onClick = {
                                                loaderConfig.themeColor.value = Color.Red
                                            }) {
                                                Text("Red")
                                            }

                                            Button(onClick = {
                                                loaderConfig.themeColor.value = Color.Blue
                                            }) {
                                                Text("Blue")
                                            }


                                        }

                                    }
                                }

                            }



                        }
                    }


                    Spacer(Modifier.height(8.dp))



                }

            }

            wizui.wizColumn(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(Color(10, 10, 10))
                .padding(16.dp)
            )
            {

            }
        }
    }


}