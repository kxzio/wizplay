package org.example.ui.uiHelpers

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val smoothEnergy = FloatArray(48)


enum class AnimSpeed { SLOW, MEDIUM, FAST }
enum class PaintStyle { FILL, OUTLINE }

data class Vec2(
    var x: Float = 0f,
    var y: Float = 0f
) {
    fun set(other: Vec2) {
        x = other.x
        y = other.y
    }
}


class BezierSpline(size: Int) {

    private val n = size - 1
    val first = Array(n) { Vec2() }
    val second = Array(n) { Vec2() }

    fun update(knots: Array<Vec2>) {
        if (knots.size == 2) {
            val x = (2 * knots[0].x + knots[1].x) / 3f
            val y = (2 * knots[0].y + knots[1].y) / 3f
            first[0].x = x; first[0].y = y
            second[0].x = 2 * x - knots[0].x
            second[0].y = 2 * y - knots[0].y
            return
        }

        val rhsX = FloatArray(n)
        val rhsY = FloatArray(n)

        rhsX[0] = knots[0].x + 2 * knots[1].x
        rhsY[0] = knots[0].y + 2 * knots[1].y

        for (i in 1 until n - 1) {
            rhsX[i] = 4 * knots[i].x + 2 * knots[i + 1].x
            rhsY[i] = 4 * knots[i].y + 2 * knots[i + 1].y
        }

        rhsX[n - 1] = (8 * knots[n - 1].x + knots[n].x) / 2f
        rhsY[n - 1] = (8 * knots[n - 1].y + knots[n].y) / 2f

        val x = solve(rhsX)
        val y = solve(rhsY)

        for (i in 0 until n) {
            first[i].x = x[i]
            first[i].y = y[i]
            if (i < n - 1) {
                second[i].x = 2 * knots[i + 1].x - x[i + 1]
                second[i].y = 2 * knots[i + 1].y - y[i + 1]
            } else {
                second[i].x = (knots[n].x + x[n - 1]) / 2f
                second[i].y = (knots[n].y + y[n - 1]) / 2f
            }
        }
    }

    private fun solve(rhs: FloatArray): FloatArray {
        val n = rhs.size
        val x = FloatArray(n)
        val tmp = FloatArray(n)

        var b = 2f
        x[0] = rhs[0] / b

        for (i in 1 until n) {
            tmp[i] = 1f / b
            b = (if (i < n - 1) 4f else 3.5f) - tmp[i]
            x[i] = (rhs[i] - x[i - 1]) / b
        }

        for (i in 1 until n) {
            x[n - i - 1] -= tmp[n - i] * x[n - i]
        }
        return x
    }
}

private fun envelope(
    bytes: ByteArray,
    index: Int,
    window: Int = 6
): Float {
    var sum = 0f
    var count = 0

    for (i in -window..window) {
        val idx = (index + i).coerceIn(0, bytes.lastIndex)
        val v = bytes[idx].toInt()
        sum += v * v
        count++
    }

    return kotlin.math.sqrt(sum / count) / 128f
}


@Composable
fun BlobVisualizer(
    audioBytes: ByteArray?,
    modifier: Modifier = Modifier,
    density: Float = 0.8f,
    color: Color,
    fill: Boolean = true,
    speed: Float = 0.003f
) {
    val nPoints = 48  // Должен совпадать с BLOB_POINTS

    val points = remember { Array(nPoints + 2) { Vec2() } }
    val spline = remember { BezierSpline(points.size) }
    val path = remember { Path() }

    var radius by remember { mutableStateOf(-1f) }

    Canvas(modifier) {
        if (audioBytes == null || audioBytes.isEmpty() || audioBytes.size < nPoints) return@Canvas

        val cx = size.width / 2
        val cy = size.height / 2
        val h = size.height

        if (radius < 0f) {
            radius = minOf(size.width, size.height) * 0.65f / 2f

            val step = (2 * Math.PI / nPoints).toFloat()
            var a = Math.PI.toFloat()  // сдвиг для баса "внизу"
            for (i in 0 until nPoints) {
                points[i].x = cx + radius * kotlin.math.cos(a)
                points[i].y = cy + radius * kotlin.math.sin(a)
                a += step
            }
        }

        // Изменено: Чтение как unsigned (0-255), без +128
        val energies = FloatArray(nPoints)
        for (i in 0 until nPoints) {
            energies[i] = audioBytes[i].toUByte().toInt() / 255f  // Теперь 0.0f до 1.0f напрямую
        }

        val step = (2 * Math.PI / nPoints).toFloat()
        var angle = Math.PI.toFloat()

        for (i in 0 until nPoints) {
            var energy = energies[i]
            if (energy < 0.02f) energy = 0f  // Шумоподавление (можно скорректировать)

            val t = energy * (h * 0.35f)  // Масштаб по энергии

            val tx = cx + (radius + t) * kotlin.math.cos(angle)
            val ty = cy + (radius + t) * kotlin.math.sin(angle)

            val lerpFactor = 0.5f  // Оставляем как есть, или уменьшите для плавности

            points[i].x = points[i].x * (1f - lerpFactor) + tx * lerpFactor
            points[i].y = points[i].y * (1f - lerpFactor) + ty * lerpFactor

            angle += step
        }

        points[nPoints].set(points[0])
        points[nPoints + 1].set(points[0])

        spline.update(points)

        path.reset()
        path.moveTo(points[0].x, points[0].y)

        for (i in spline.first.indices) {
            path.cubicTo(
                spline.first[i].x, spline.first[i].y,
                spline.second[i].x, spline.second[i].y,
                points[i + 1].x, points[i + 1].y
            )
        }

        if (fill) path.lineTo(cx, cy)  // Заполнение, если нужно

        drawPath(
            path,
            color,
            style = if (fill) Fill else Stroke(2.dp.toPx())
        )
    }
}

enum class VisualizerStyle {
    BARS,
    WAVE
}

@Composable
fun BarVisualizer(
    audioBytes: ByteArray?,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    density: Float = 1.0f,
    gravityBottom: Boolean = true,
    style: VisualizerStyle = VisualizerStyle.BARS
) {
    val MAX_BARS = 120
    val MIN_BARS = 8

    val nBars = remember {
        (MAX_BARS * density).toInt().coerceAtLeast(MIN_BARS)
    }

    val srcY = remember { FloatArray(nBars) }
    val dstY = remember { FloatArray(nBars) }
    val smoothY = remember { FloatArray(nBars) }

    var initialized by remember { mutableStateOf(false) }

    Canvas(modifier) {
        if (audioBytes == null || audioBytes.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val barWidth = w / nBars

        val minY = if (gravityBottom) h * 0.05f else 0f
        val maxY = if (gravityBottom) h else h * 0.95f

        // ---------- INIT ----------
        if (!initialized) {
            val base = if (gravityBottom) h else 0f
            for (i in 0 until nBars) {
                srcY[i] = base
                dstY[i] = base
            }
            initialized = true
        }

        // ---------- TARGET UPDATE ----------
        for (i in 0 until nBars) {
            val frac = i.toFloat() / (nBars - 1) * (audioBytes.size - 1)
            val lo = frac.toInt().coerceIn(0, audioBytes.lastIndex)
            val hi = (lo + 1).coerceIn(0, audioBytes.lastIndex)
            val t = frac - lo

            val v0 = audioBytes[lo].toUByte().toInt()
            val v1 = audioBytes[hi].toUByte().toInt()
            val v = v0 + (v1 - v0) * t

            val amp = v / 255f
            val barHeight = amp * h * 0.9f
            val y = if (gravityBottom) h - barHeight else barHeight

            dstY[i] = y.coerceIn(minY, maxY)
        }

        // ---------- TEMPORAL SMOOTH ----------
        val lerp = 0.2f
        for (i in 0 until nBars) {
            srcY[i] += (dstY[i] - srcY[i]) * lerp
            srcY[i] = srcY[i].coerceIn(minY, maxY)
        }

        // ---------- SPATIAL SMOOTH ----------
        for (i in 0 until nBars) {
            val p = srcY.getOrNull(i - 1) ?: srcY[i]
            val c = srcY[i]
            val n = srcY.getOrNull(i + 1) ?: srcY[i]
            smoothY[i] = (p + c * 2f + n) / 4f
        }

        when (style) {

            // ================= BARS =================
            VisualizerStyle.BARS -> {
                for (i in 0 until nBars) {
                    val x = i * barWidth + barWidth / 2f
                    drawLine(
                        color = color,
                        start = if (gravityBottom)
                            Offset(x, h)
                        else
                            Offset(x, 0f),
                        end = Offset(x, srcY[i]),
                        strokeWidth = barWidth * 0.6f,
                    )
                }
            }

            // ================= WAVE =================
            VisualizerStyle.WAVE -> {
                val path = Path()
                val tension = 0.25f   // 🔑 ключ к отсутствию overshoot

                fun y(i: Int) = smoothY
                    .getOrElse(i) { smoothY.last() }
                    .coerceIn(minY, maxY)

                for (i in 0 until nBars - 1) {
                    val x0 = i * barWidth
                    val x1 = (i + 1) * barWidth

                    val y0 = y(i)
                    val y1 = y(i + 1)
                    val yPrev = y(i - 1)
                    val yNext = y(i + 2)

                    val cx1 = x0 + barWidth / 2f
                    val cy1 = y0 + (y1 - yPrev) * tension

                    val cx2 = x1 - barWidth / 2f
                    val cy2 = y1 - (yNext - y0) * tension

                    if (i == 0) path.moveTo(x0, y0)

                    path.cubicTo(
                        cx1, cy1.coerceIn(minY, maxY),
                        cx2, cy2.coerceIn(minY, maxY),
                        x1, y1
                    )
                }

                // ---- LINE ----
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        join = StrokeJoin.Round
                    )
                )

                // ---- FILL ----
                val fill = Path().apply {
                    addPath(path)
                    if (gravityBottom) {
                        lineTo(w, h)
                        lineTo(0f, h)
                    } else {
                        lineTo(w, 0f)
                        lineTo(0f, 0f)
                    }
                    close()
                }

                drawPath(
                    path = fill,
                    color = color,
                    style = Fill
                )
            }
        }
    }
}







