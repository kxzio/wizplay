package org.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }

    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j

    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }
    return dp[a.length][b.length]
}

fun similarity(a: String, b: String): Float {
    if (a.isEmpty() || b.isEmpty()) return 0f
    val dist = levenshtein(a.lowercase(), b.lowercase())
    return 1f - dist.toFloat() / maxOf(a.length, b.length)
}

@Composable
fun FpsCounter(): MutableState<Int> {
    val fpsState = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        var frames = 0
        var lastTime = 0L

        while (true) {
            withFrameNanos { time ->
                if (lastTime == 0L) {
                    lastTime = time
                }
                frames++

                if (time - lastTime >= 1_000_000_000L) { // 1 секунда
                    fpsState.value = frames
                    frames = 0
                    lastTime = time
                }
            }
        }
    }

    return fpsState
}
