package org.example.wizui

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize.Companion
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

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
                    indication = rememberRipple(bounded = true)
                ) {
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

            AnimatedContent(
                targetState = visualPressed,
                contentAlignment = Alignment.CenterStart,
                label = "buttonContent"
            ) {
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
    }


    @Composable
    fun wizColumn(
        modifier: Modifier = Modifier,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        content: @Composable (() -> Unit))
    {
        Column(
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            modifier = modifier.animateContentSize(spring(
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ))
        ) {
            content()
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
                color = contentColor.copy(alpha = (selectedAlpha.value + 0.25f))
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
                    .clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = true),
                    ) {
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
        contentPadding: PaddingValues = PaddingValues(0.dp),
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        items: List<T>,
        state: LazyListState = rememberLazyListState(),
        itemContent: @Composable (item: T) -> Unit
    ) {
        LazyColumn(
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
        columns: Int,
        items: List<T>,
        state: LazyGridState = rememberLazyGridState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        itemContent: @Composable (item: T) -> Unit
    ) {
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Fixed(columns),
            state = state,
            contentPadding = contentPadding
        ) {
            itemsIndexed(items, key = { _, item -> item.hashCode() }) { index, item ->
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
    fun wizSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        modifier: Modifier = Modifier,
        sliderColors: SliderColors = SliderDefaults.colors(),
        tickColor: Color = Color.White.copy(alpha = 0.3f),
        tickHeight: Dp = 8.dp,
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {

            /* ───────────── Деления (верх + низ) ───────────── */

            val horizontalPadding = 10.dp
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                val left = horizontalPadding.toPx()
                val right = size.width - horizontalPadding.toPx()
                val usableWidth = right - left

                val tickCount = steps + 1
                val stepPx = usableWidth / tickCount

                repeat(tickCount + 1) { i ->
                    val x = left + stepPx * i

                    // верх
                    drawLine(
                        color = tickColor,
                        start = Offset(x, 0f),
                        end = Offset(x, tickHeight.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // низ
                    drawLine(
                        color = tickColor,
                        start = Offset(x, size.height),
                        end = Offset(x, size.height - tickHeight.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }


            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                interactionSource = interactionSource,
                colors = sliderColors,
                modifier = Modifier.fillMaxWidth()
                    .align(Alignment.Center),
                track = {
                    SliderDefaults.Track(
                        sliderState = it,
                        colors = sliderColors,
                    )
                },
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
                translationY = offsetY.toPx()
            }
        ) {
            content()
        }
    }



}
