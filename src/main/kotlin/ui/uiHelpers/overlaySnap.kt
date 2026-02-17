package org.example.ui.uiHelpers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun snapCornerOverlay(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isDrag : MutableState<Boolean>) -> Unit
) {
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier.fillMaxSize()) {

        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        var overlayWidthPx by remember { mutableStateOf<Float?>(null) }
        var overlayHeightPx by remember { mutableStateOf<Float?>(null) }

        // не создаём maxX/maxY пока нет размера
        val maxX = remember(containerWidthPx, overlayWidthPx) {
            overlayWidthPx?.let { containerWidthPx - it }
        }

        val maxY = remember(containerHeightPx, overlayHeightPx) {
            overlayHeightPx?.let { containerHeightPx - it }
        }

        var normalizedX by remember { mutableStateOf(1f) }
        var normalizedY by remember { mutableStateOf(1f) }

        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }

        // ИНИЦИАЛИЗАЦИЯ — только когда размер впервые появился
        LaunchedEffect(maxX, maxY) {

            if (maxX != null && maxY != null && offsetX.value == 0f && offsetY.value == 0f) {

                offsetX.snapTo(normalizedX * maxX)
                offsetY.snapTo(normalizedY * maxY)
            }
        }

        fun updateNormalized() {
            if (maxX != null && maxY != null) {
                normalizedX = offsetX.value / maxX
                normalizedY = offsetY.value / maxY
            }
        }

        fun nearestAnchor(current: Offset): Offset {

            val mx = maxX ?: return current
            val my = maxY ?: return current

            val anchors = listOf(
                Offset(0f, 0f),
                Offset(mx, 0f),
                Offset(0f, my),
                Offset(mx, my)
            )

            return anchors.minBy {
                hypot(it.x - current.x, it.y - current.y)
            }
        }

        val isDrag = remember { mutableStateOf(false) }

        Box(
            Modifier
                .offset {
                    IntOffset(
                        offsetX.value.toInt(),
                        offsetY.value.toInt()
                    )
                }

                // только измеряем, НЕ двигаем
                .onSizeChanged {

                    overlayWidthPx = it.width.toFloat()
                    overlayHeightPx = it.height.toFloat()
                }

                .pointerInput(maxX, maxY) {

                    if (maxX == null || maxY == null) return@pointerInput

                    detectDragGestures(

                        onDrag = { change, drag ->

                            isDrag.value = true

                            change.consume()

                            scope.launch {

                                offsetX.snapTo(
                                    (offsetX.value + drag.x)
                                        .coerceIn(0f, maxX)
                                )

                                offsetY.snapTo(
                                    (offsetY.value + drag.y)
                                        .coerceIn(0f, maxY)
                                )

                                updateNormalized()
                            }
                        },

                        onDragEnd = {

                            val nearest = nearestAnchor(
                                Offset(offsetX.value, offsetY.value)
                            )

                            scope.launch {
                                offsetX.animateTo(
                                    nearest.x,
                                    spring(stiffness = Spring.StiffnessMediumLow)
                                )
                                updateNormalized()
                            }

                            scope.launch {
                                offsetY.animateTo(
                                    nearest.y,
                                    spring(stiffness = Spring.StiffnessMediumLow)
                                )
                                updateNormalized()
                            }

                            isDrag.value = false
                        }
                    )
                }
        ) {
            content(isDrag)
        }
    }
}

