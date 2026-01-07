package org.example.wizui

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowDropDown
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.ripple.rememberRipple

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Cursor

fun Color.toHexString(includeAlpha: Boolean = true): String {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return if (includeAlpha) {
        String.format("#%02X%02X%02X%02X", a, r, g, b)
    } else {
        String.format("#%02X%02X%02X", r, g, b)
    }
}


data class HSV(
    val h: Float, // 0..360
    val s: Float, // 0..1
    val v: Float  // 0..1
)

fun hsvToColor(h: Float, s: Float, v: Float): Color =
    Color.hsv(
        h.coerceIn(0f, 360f),
        s.coerceIn(0f, 1f),
        v.coerceIn(0f, 1f)
    )

fun Color.toHSV(): HSV {
    val r = red
    val g = green
    val b = blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta) % 6f
        max == g -> ((b - r) / delta) + 2f
        else -> ((r - g) / delta) + 4f
    } * 60f

    val h = if (hue < 0) hue + 360f else hue
    val s = if (max == 0f) 0f else delta / max
    val v = max

    return HSV(
        h.coerceIn(0f, 360f),
        s.coerceIn(0f, 1f),
        v.coerceIn(0f, 1f)
    )
}

/*
    wizui interface main class
*/
object wizui {

    //wizui - start

    @Composable
    fun wizButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,

        shape: Shape = MaterialTheme.shapes.small,
        backgroundColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        contentColorToggled: Color = backgroundColor,

        turnOffToggleIndication: Boolean = false,
        toggleVariable: Boolean? = null,
        toggleIconWhenOff: ImageVector = Icons.Sharp.Close,

        delayedClick: Boolean = false,
        delayedClickDurationMs: Long = 150,

        content: @Composable (() -> Unit),
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val scope = rememberCoroutineScope()

        val pressed by interactionSource.collectIsPressedAsState()
        var forcedPressed by remember { mutableStateOf(false) }

        val visualPressed = pressed || forcedPressed

        /* ───────────── Анимации ───────────── */

        val scaleOfButton = animateFloatAsState(
            targetValue = if (visualPressed) 0.97f else 1f,
            animationSpec = tween(120, easing = FastOutSlowInEasing),
            label = "scale"
        )

        val borderAlpha = animateFloatAsState(
            targetValue = if (toggleVariable == true) 1f else 0f,
            animationSpec = tween(320, easing = FastOutSlowInEasing),
            label = "border"
        )

        /* ───────────── Контейнер ───────────── */

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .graphicsLayer {
                    scaleX = scaleOfButton.value
                    scaleY = scaleOfButton.value
                }
                .background(
                    backgroundColor.copy(
                        alpha = backgroundColor.alpha -
                                (backgroundColor.alpha * borderAlpha.value) / 1.4f
                    ),
                    shape
                )
                .border(
                    BorderStroke(0.3.dp, backgroundColor.copy(alpha = borderAlpha.value)),
                    shape
                )
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                )
                 {
                    if (!delayedClick) {
                        onClick()
                    } else {
                        scope.launch {
                            forcedPressed = true
                            delay(delayedClickDurationMs)
                            onClick()
                            forcedPressed = false
                        }
                    }
                }
        ) {

            /* ───────────── Контент ───────────── */


                Row(modifier = Modifier.height(IntrinsicSize.Min)) {

                    if (toggleVariable == true && !turnOffToggleIndication) {
                        Column(
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .background(backgroundColor),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                toggleIconWhenOff,
                                contentDescription = "",
                                tint = contentColor
                            )
                        }
                    }

                    Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        CompositionLocalProvider(
                            LocalContentColor provides
                                    if (toggleVariable == true) contentColorToggled
                                    else contentColor
                        ) {
                            content()
                        }
                    }
                }

        }
    }

    fun Modifier.cursorForHorizontalResize(): Modifier =
        this.pointerHoverIcon(
            PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
        )


    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun wizColumn(
        modifier: Modifier = Modifier,
        resizable: Boolean = false,

        minWidthFraction: Float = 0.2f,
        maxWidthFraction: Float = 0.6f,
        initialWidthFraction: Float = 0.5f,

        parentMaxWidth: Dp = 1.dp,

        content: @Composable () -> Unit
    ) {
        val density = LocalDensity.current

        var widthFraction by remember {
            mutableStateOf(
                initialWidthFraction.coerceIn(
                    minWidthFraction,
                    maxWidthFraction
                )
            )
        }

        val widthDp = parentMaxWidth * widthFraction

        val animatedWidthDp by animateDpAsState(
            widthDp,
            label = "wizColumnWidth"
        )

        Box(
            modifier = modifier.then(
                if (resizable) Modifier.width(animatedWidthDp)
                else Modifier
            )
        ) {

            Column(Modifier.fillMaxSize()) {
                content()
            }

            var hovered by remember { mutableStateOf(false) }
            var drag by remember { mutableStateOf(false) }

            var alpha = animateIntAsState(
                targetValue = if (hovered || drag) 30 else 0
            )

            if (resizable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(6.dp)
                        .onPointerEvent(PointerEventType.Enter) {
                            hovered = true
                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            hovered = false
                        }
                        .fillMaxHeight()
                        .background(Color(255, 255, 255, alpha.value))
                        .cursorForHorizontalResize()
                        .pointerInput(parentMaxWidth, density) {
                            detectDragGestures(
                                onDragStart = {
                                    drag = true
                                },
                                onDragEnd = {
                                    drag = false
                                },
                                onDragCancel = {
                                    drag = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    val deltaFraction =
                                        with(density) { dragAmount.x.toDp() / parentMaxWidth }

                                    widthFraction =
                                        (widthFraction + deltaFraction)
                                            .coerceIn(
                                                minWidthFraction,
                                                maxWidthFraction
                                            )
                                }
                            )
                        }

                )
            }
        }
    }




    @Composable
    fun AnimatedCheckToCloseIcon(
        isClose: Boolean,
        color: Color,
        strokeWidth: Float = 4f,
        modifier: Modifier = Modifier
    ) {
        val progress by animateFloatAsState(
            targetValue = if (isClose) 1f else 0f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )

        Canvas(modifier) {
            val w = size.width
            val h = size.height

            // --- Галочка ---
            val checkLine1Start = Offset(w * 0.25f, h * 0.55f) // нижний левый
            val checkLine1End   = Offset(w * 0.45f, h * 0.75f) // центр снизу

            val checkLine2Start = Offset(w * 0.45f, h * 0.75f) // центр снизу
            val checkLine2End   = Offset(w * 0.75f, h * 0.35f) // верх справа

            // --- Крест ---
            val crossLine1Start = Offset(w * 0.25f, h * 0.25f)
            val crossLine1End   = Offset(w * 0.75f, h * 0.75f)

            val crossLine2Start = Offset(w * 0.75f, h * 0.25f)
            val crossLine2End   = Offset(w * 0.25f, h * 0.75f)

            // Линия 1
            val line1Start = lerp(checkLine1Start, crossLine1Start, progress)
            val line1End   = lerp(checkLine1End,   crossLine1End,   progress)

            // Линия 2
            val line2Start = lerp(checkLine2Start, crossLine2Start, progress)
            val line2End   = lerp(checkLine2End,   crossLine2End,   progress)

            drawLine(color, line1Start, line1End, strokeWidth, cap = StrokeCap.Round)
            drawLine(color, line2Start, line2End, strokeWidth, cap = StrokeCap.Round)
        }
    }

    fun lerp(start: Offset, end: Offset, fraction: Float): Offset {
        return Offset(
            start.x + (end.x - start.x) * fraction,
            start.y + (end.y - start.y) * fraction
        )
    }

    @Composable
    fun wizCheckBox(
        text: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        boxSize: Dp = 26.dp,
        checkSize: Dp = 16.dp,
        backgroundColor: Color = MaterialTheme.colorScheme.background,
        activeBackgroundColor: Color = MaterialTheme.colorScheme.primary,
        checkmarkColor: Color = MaterialTheme.colorScheme.background,
        contentColor: Color = Color.White
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(4.dp)
        ) {

            val checkedAlpha = animateFloatAsState(
                targetValue = if (checked) activeBackgroundColor.alpha else 0f
            )

            Box(
                modifier = Modifier
                    .size(boxSize)
                    .background(
                        backgroundColor,
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(boxSize)
                        .background(
                            activeBackgroundColor.copy(alpha = checkedAlpha.value),
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    AnimatedCheckToCloseIcon(
                        strokeWidth = 2.5f,
                        color = checkmarkColor.copy(alpha = checkedAlpha.value),
                        modifier = Modifier.size(checkSize),
                        isClose = false,
                    )
                }

            }

            Spacer(Modifier.width(12.dp))

            Text(text, color = contentColor.copy(alpha = (((checkedAlpha.value) + 0.25f)))  )
        }
    }

    @Composable
    fun wizRadioButton(
        text: String,
        selected: Boolean,
        onSelect: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        circleSize: Dp = 22.dp,
        backgroundColor: Color = MaterialTheme.colorScheme.background,
        activeBackgroundColor: Color = MaterialTheme.colorScheme.primary,
        dotColor: Color = MaterialTheme.colorScheme.background,
        contentColor: Color = Color.White
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clickable(enabled = enabled) { onSelect() }
                .padding(4.dp)
        ) {
            val selectedAlpha = animateFloatAsState(
                targetValue = if (selected) 1f else 0f
            )

            Box(
                modifier = Modifier
                    .size(circleSize),
                contentAlignment = Alignment.Center
            ) {

                AnimatedCheckToCloseIcon(
                    isClose = !selected,
                    color = if (selected) activeBackgroundColor else backgroundColor,
                    strokeWidth = 2.5f,
                    modifier = Modifier
                        .size(circleSize),
                )


            }

            Spacer(Modifier.width(12.dp))

            Text(
                text,
                color = contentColor.copy(alpha = (selectedAlpha.value + 0.35f))
            )
        }
    }


    @Composable
    fun wizBox(
        modifier: Modifier = Modifier,
        contentAlignment: Alignment = Alignment.TopStart,
        propagateMinConstraints: Boolean = false,
        content: @Composable (() -> Unit))
    {
        Box(
            contentAlignment = contentAlignment,
            propagateMinConstraints = propagateMinConstraints,
            modifier = modifier.animateContentSize(spring(
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ))
        ) {
            content()
        }

    }

    enum class WizAnimationType {
        Fade,
        SlideLeft,
        SlideRight,
        SlideUp,
        SlideDown,
        ExpandHorizontally,
        ExpandVertically,
    }

    @Composable
    fun wizBlinkingText(
        text: String,
        modifier: Modifier = Modifier,
        normalColor: Color = Color.White,
        blinkColor: Color = Color.Red,
        fontSize: TextUnit = 22.sp,
        onClick: () -> Unit
    ) {
        val animColor = remember { Animatable(normalColor) }
        val scope = rememberCoroutineScope()
        val interactionSource = remember { MutableInteractionSource() }

        Text(
            text = text,
            color = animColor.value,
            fontSize = fontSize,
            modifier = modifier.clickable(
                interactionSource = interactionSource,
                indication = null // убираем ripple
            ) {
                onClick()

                scope.launch {
                    animColor.stop()
                    animColor.animateTo(
                        blinkColor,
                        animationSpec = tween(320)
                    )
                    animColor.animateTo(
                        normalColor,
                        animationSpec = tween(380)
                    )
                }
            }
        )
    }


    @Composable
    fun Boolean.wizAnimateIf(
        type: WizAnimationType = WizAnimationType.Fade,
        speedIn : Int = 450,
        speedOut : Int = 300,
        content: @Composable () -> Unit
    ) {
        val enter: EnterTransition
        val exit: ExitTransition

        when (type) {
            WizAnimationType.Fade -> {
                enter = fadeIn(tween(speedIn))
                exit = fadeOut(tween(speedOut))
            }
            WizAnimationType.SlideLeft -> {
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(speedIn)
                )
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(speedOut)
                )
            }
            WizAnimationType.SlideRight -> {
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(speedIn)
                )
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(speedOut)
                )
            }
            WizAnimationType.SlideUp -> {
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(speedIn)
                )
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(speedOut)
                )
            }
            WizAnimationType.SlideDown -> {
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(speedIn)
                )
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(speedOut)
                )
            }
            WizAnimationType.ExpandHorizontally -> {
                enter = expandHorizontally(
                    animationSpec = tween(speedIn)
                ) + fadeIn(tween(speedIn))
                exit = shrinkHorizontally(
                    animationSpec = tween(speedOut)
                ) + fadeOut(tween(speedOut))
            }


            WizAnimationType.ExpandVertically -> {
                enter = expandVertically (
                    animationSpec = tween(speedIn)
                ) + fadeIn(tween(speedIn))
                exit = shrinkVertically (
                    animationSpec = tween(speedOut)
                ) + fadeOut(tween(speedOut))
            }


        }

        AnimatedVisibility(
            visible = this,
            enter = enter,
            exit = exit
        ) {
            content()
        }
    }

    @Composable
    fun wizSelector(
        list: List<String>,
        targetVariable: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier = Modifier,
        shape: Shape = MaterialTheme.shapes.small,
        backgroundColor: Color = MaterialTheme.colorScheme.primary,
        dropdownColor: Color = MaterialTheme.colorScheme.background,
        notSelectedColor : Color = Color(255, 255, 255),
        selectedColor : Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        contentColorToggled: Color = backgroundColor,
    ) {
        var isOpened by remember { mutableStateOf(false) }

        val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()

        val scaleOfButton = animateFloatAsState(
            targetValue = if (pressed) 0.97f else 1f,
            animationSpec = tween(120, easing = FastOutSlowInEasing)
        )

        val borderAlpha = animateFloatAsState(
            targetValue = if (isOpened) 1f else 0f,
            animationSpec = tween(320, easing = FastOutSlowInEasing)
        )

        Column {

            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .graphicsLayer {
                        scaleX = scaleOfButton.value
                        scaleY = scaleOfButton.value
                    }
                    .background(
                        backgroundColor.copy(
                            alpha = backgroundColor.alpha - (backgroundColor.alpha * borderAlpha.value) / 1.4f
                        ),
                        shape
                    )
                    .border(BorderStroke(0.3.dp, backgroundColor.copy(alpha = borderAlpha.value)), shape)
                    .clickable
                    {
                        isOpened = !isOpened
                    }
                    .clip(shape)
            ) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .background(backgroundColor),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val iconRotate = animateFloatAsState(
                            targetValue = if (isOpened) -180f else 0f,
                            animationSpec = tween(320, easing = FastOutSlowInEasing)
                        )
                        Icon(Icons.Sharp.ArrowDropDown, contentDescription = "", tint = contentColor, modifier = Modifier.rotate(iconRotate.value))
                    }

                    Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (isOpened) contentColorToggled else contentColor
                        ) {
                            // Кнопка
                            AnimatedContent(
                                targetState = targetVariable,
                                contentAlignment = Alignment.Center,
                                label = "buttonContent"
                            ) { state ->

                                Text(list[targetVariable])
                            }
                        }
                    }
                }
            }

            Column(Modifier.padding(start = 16.dp)) {
                if (isOpened) {
                    DropdownMenu(
                        modifier = Modifier.background(dropdownColor),
                        expanded = isOpened,
                        onDismissRequest = { isOpened = false }
                    ) {
                        list.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = { Text(item, color = if (index == targetVariable) selectedColor else notSelectedColor) },
                                onClick = {
                                    onSelect(index)
                                    isOpened = false
                                }
                            )
                        }
                    }
                }
            }


        }


    }


    @Composable
    fun WizAnimatedLazyItem(
        index: Int,
        state: LazyListState,
        content: @Composable () -> Unit
    ) {
        val layoutInfo = state.layoutInfo
        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

        val isVisible = itemInfo != null

        val alpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (isVisible) 450 else 300
            ),
            label = "alpha"
        )

        val offsetX by animateDpAsState(
            targetValue = if (isVisible) 0.dp else 24.dp,
            animationSpec = tween(
                durationMillis = if (isVisible) 450 else 300
            ),
            label = "offset"
        )

        Box(
            Modifier
                .graphicsLayer {
                    this.alpha = alpha
                    translationX = offsetX.toPx()
                }
        ) {
            content()
        }
    }


    @Composable
    fun <T> wizVerticalList(
        modifier: Modifier = Modifier,
        userScrollEnabled: Boolean = true,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        items: List<T>,
        state: LazyListState = rememberLazyListState(),
        itemContent: @Composable (item: T) -> Unit
    ) {
        LazyColumn(
            userScrollEnabled = userScrollEnabled,
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment
        ) {
            itemsIndexed(items, key = { _, item -> item.hashCode() }) { index, item ->
                WizAnimatedLazyItem(
                    index = index,
                    state = state
                ) {
                    itemContent(item)
                }
            }
        }
    }


    @Composable
    fun <T> wizVerticalGrid(
        modifier: Modifier = Modifier,
        columns: Int = 1,
        key: ((item: T) -> Any)? = null,
        dynamicColumnsCount: Boolean = false,
        dynamicMinSizeForElement: Dp = 0.dp,
        userScrollEnabled: Boolean = false,
        items: List<T>,
        state: LazyGridState = rememberLazyGridState(),
        horizontalArrangement: Arrangement.Horizontal,
        verticalArrangement: Arrangement.Vertical,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        itemContent: @Composable (item: T) -> Unit,
    ) {
        LazyVerticalGrid(
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            userScrollEnabled = userScrollEnabled,
            modifier = modifier,
            columns =
                if (!dynamicColumnsCount)
                    GridCells.Fixed(columns)
                else
                    GridCells.Adaptive(dynamicMinSizeForElement)
            ,
            state = state,
            contentPadding = contentPadding
        ) {
            itemsIndexed(items, key = { _, item -> key?.invoke(item) ?: item.hashCode() }) { index, item ->
                WizAnimatedGridItem(
                    index = index,
                    state = state
                ) {
                    itemContent(item)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FlatSliderTrack(
        sliderState: SliderState,
        steps: Int = sliderState.steps,
        colors: SliderColors,
        height: Dp = 4.dp,
        tickColor: Color = Color.White.copy(alpha = 0.3f),
        tickHeight: Dp = 12.dp,
        tickOffsetY: Dp = 18.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxOf(height, tickHeight * 2))
        ) {
            val trackHeightPx = height.toPx()
            val tickHeightPx = tickHeight.toPx()

            val centerY = size.height / 2
            val trackTop = centerY - trackHeightPx / 2

            /* ───────────── Fraction ───────────── */

            val fraction =
                (sliderState.value - sliderState.valueRange.start) /
                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start)

            val clamped = fraction.coerceIn(0f, 1f)

            /* ───────────── Track ───────────── */

            // inactive
            drawRect(
                color = colors.inactiveTrackColor,
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeightPx)
            )

            // active
            drawRect(
                color = colors.activeTrackColor,
                topLeft = Offset(0f, trackTop),
                size = Size(size.width * clamped, trackHeightPx)
            )

            /* ───────────── Ticks ───────────── */

            if (steps > 0) {
                val tickCount = steps + 1
                val stepPx = size.width / tickCount

                repeat(tickCount + 1) { i ->
                    val x = stepPx * i

                    // верхний tick
                    drawLine(
                        color = tickColor,
                        start = Offset(x, trackTop - tickOffsetY.toPx() - tickHeightPx),
                        end = Offset(x, trackTop - tickOffsetY.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // нижний tick
                    drawLine(
                        color = tickColor,
                        start = Offset(x, trackTop + trackHeightPx + tickOffsetY.toPx()),
                        end = Offset(x, trackTop + trackHeightPx + tickHeightPx + tickOffsetY.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun wizSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        modifier: Modifier = Modifier,
        sliderColors: SliderColors = SliderDefaults.colors(),
        tickColor: Color = Color.White.copy(alpha = 0.3f),
        tickHeight: Dp = 8.dp,
        disableRealSliderSteps : Boolean = false
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = if (disableRealSliderSteps) 0 else steps,
                interactionSource = interactionSource,
                colors = sliderColors,
                modifier = Modifier.fillMaxWidth()
                    .align(Alignment.Center),
                track = {
                    FlatSliderTrack(
                        steps = if (disableRealSliderSteps) steps else it.steps,
                        sliderState = it,
                        colors = sliderColors
                    )
                },
                thumb = {
                    Box(
                        Modifier
                            .size(4.dp, 21.dp)
                            .background(Color.White)
                    )
                }
            )
        }
    }





    @Composable
    fun WizAnimatedGridItem(
        index: Int,
        state: LazyGridState,
        content: @Composable () -> Unit
    ) {
        val isVisible = state.layoutInfo.visibleItemsInfo.any {
            it.index == index
        }

        val alpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(450),
            label = "alpha"
        )

        val offsetY by animateDpAsState(
            targetValue = if (isVisible) 0.dp else 16.dp,
            animationSpec = tween(450),
            label = "offset"
        )

        Box(
            Modifier.graphicsLayer {
                this.alpha = alpha
            }
        ) {
            content()
        }
    }


    @Composable
    fun SVSquare(
        hue: Float,
        saturation: Float,
        value: Float,
        onChangeStart: () -> Unit,
        onChangeEnd: () -> Unit,
        onChange: (Float, Float) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.White, Color.hsv(hue, 1f, 1f))
                    )
                )
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black)
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onChangeStart() },
                        onDragEnd = { onChangeEnd() },
                        onDragCancel = { onChangeEnd() }
                    ) { change, _ ->
                        val x = change.position.x / size.width
                        val y = change.position.y / size.height
                        onChange(
                            x.coerceIn(0f, 1f),
                            (1f - y).coerceIn(0f, 1f)
                        )
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(
                        saturation * size.width,
                        (1f - value) * size.height
                    ),
                    style = Stroke(2.dp.toPx())
                )
            }
        }
    }


    @Composable
    fun HueSlider(
        hue: Float,
        onChangeStart: () -> Unit,
        onChangeEnd: () -> Unit,
        onHueChange: (Float) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onChangeStart() },
                        onDragEnd = { onChangeEnd() },
                        onDragCancel = { onChangeEnd() }
                    ) { change, _ ->
                        val y = (change.position.y / size.height)
                            .coerceIn(0f, 1f)
                        onHueChange(y * 360f)
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val y = (hue / 360f) * size.height
                drawRect(
                    color = Color.White,
                    topLeft = Offset(0f, y - 2.dp.toPx()),
                    size = Size(size.width, 4.dp.toPx())
                )
            }
        }
    }



    @Composable
    fun wizColorPicker(
        initialColor: Color,
        onColorChanged: (Color) -> Unit,
        modifier: Modifier = Modifier,
        height: Dp,
    ) {
        var hue by remember { mutableStateOf(0f) }
        var saturation by remember { mutableStateOf(1f) }
        var value by remember { mutableStateOf(1f) }

        // 🔒 последний валидный hue (как в Photoshop)
        var lastValidHue by remember { mutableStateOf(0f) }

        // 🧲 флаг, чтобы не пересчитывать hue во время drag
        var isDragging by remember { mutableStateOf(false) }

        // инициализация ТОЛЬКО извне
        LaunchedEffect(initialColor.toArgb()) {
            if (!isDragging) {
                val hsv = initialColor.toHSV()

                saturation = hsv.s
                value = hsv.v

                if (hsv.s > 0f) {
                    hue = hsv.h
                    lastValidHue = hsv.h
                } else {
                    hue = lastValidHue
                }
            }
        }

        Row(modifier) {

            // 🔳 SV квадрат
            SVSquare(
                hue = hue,
                saturation = saturation,
                value = value,
                onChangeStart = { isDragging = true },
                onChangeEnd = { isDragging = false },
                onChange = { s, v ->
                    saturation = s
                    value = v

                    if (s > 0f) {
                        lastValidHue = hue
                    }

                    onColorChanged(hsvToColor(hue, s, v))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(height)

            )

            Spacer(Modifier.width(12.dp))

            // 🎚 Hue slider
            HueSlider(
                hue = hue,
                onChangeStart = { isDragging = true },
                onChangeEnd = { isDragging = false },
                onHueChange = {
                    hue = it
                    lastValidHue = it
                    onColorChanged(hsvToColor(it, saturation, value))
                },
                modifier = Modifier
                    .width(28.dp)
                    .height(height)
            )
        }
    }


}
