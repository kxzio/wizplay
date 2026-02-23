package ui.screens.leftPager.audioSources.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.v2.maxScrollOffset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberScrollFraction(listState: LazyListState): Float {

    val adapter = rememberScrollbarAdapter(listState)

    val fraction by remember {
        derivedStateOf {

            val max = adapter.maxScrollOffset

            if (max <= 0.0) 0f
            else (adapter.scrollOffset / max).toFloat()
        }
    }

    return fraction.coerceIn(0f, 1f)
}

@Composable
fun ScrollProgressThumb(
    scrollFraction: Float,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 36.dp
) {
    var containerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    Box(
        modifier = modifier
            .padding(top = 76.dp, bottom = 28.dp)
            .width(1.dp)
            .fillMaxHeight()
            .onSizeChanged {
                containerHeightPx = it.height
            }
    ) {
        // ───── Track ─────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.White.copy(alpha = 0.10f),
                )
        )

        if (containerHeightPx > 0) {
            val maxOffset =
                (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)

            val thumbOffsetY =
                maxOffset * scrollFraction.coerceIn(0f, 1f)

            // ───── Thumb ─────
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffsetY.toInt()) }
                    .width(3.dp)
                    .height(thumbHeight)
                    .background(
                        MaterialTheme.colorScheme.primary,
                    )
            )
        }
    }
}

@Composable
fun AlphabetBubble(letter: Char) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter.toString(),
                fontSize = 48.sp,
                color = Color.White
            )
        }
    }
}

fun handleAlphabetTouch(
    y: Float,
    heightPx: Int,
    letters: List<Char>,
    letterToIndex: Map<Char, Int>,
    scope: CoroutineScope,
    listState: LazyListState,
    onLetterChanged: (Char) -> Unit
) {
    if (heightPx == 0) return

    val letterHeight = heightPx / letters.size
    val index = (y / letterHeight)
        .toInt()
        .coerceIn(0, letters.lastIndex)

    val letter = letters[index]
    onLetterChanged(letter)

    val targetIndex = letterToIndex[letter] ?: return
    scope.launch {
        listState.scrollToItem(targetIndex)
    }
}
