package org.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

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

fun Double.toTimeString(): String {
    if (this.isNaN() || this <= 0) return "0:00"

    val totalSeconds = this.toInt()
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
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



fun dominantColorFromPathStable(
    path: String,
    targetSize: Int = 64,
    clustersCount: Int = 5
): Int {

    data class P(val r: Float, val g: Float, val b: Float)

    fun clampSaturation(
        c: P,
        minSaturation: Float = 0.35f
    ): P {
        val max = maxOf(c.r, c.g, c.b)
        val min = minOf(c.r, c.g, c.b)
        val currentSat = if (max == 0f) 0f else (max - min) / max

        if (currentSat >= minSaturation) return c

        val avg = (c.r + c.g + c.b) / 3f
        val boost = minSaturation / max(currentSat, 0.001f)

        return P(
            (avg + (c.r - avg) * boost).coerceIn(0f, 255f),
            (avg + (c.g - avg) * boost).coerceIn(0f, 255f),
            (avg + (c.b - avg) * boost).coerceIn(0f, 255f)
        )
    }

    fun saturation(p: P): Float {
        val max = max(p.r, max(p.g, p.b))
        val min = min(p.r, min(p.g, p.b))
        return if (max == 0f) 0f else (max - min) / max
    }

    fun brightness(p: P): Float =
        max(p.r, max(p.g, p.b)) / 255f

    fun clampColor(p: P): P {
        val minBrightness = 0.25f
        val minSaturation = 0.25f

        var r = p.r
        var g = p.g
        var b = p.b

        val br = brightness(p)
        if (br < minBrightness) {
            val factor = minBrightness / max(br, 0.001f)
            r *= factor
            g *= factor
            b *= factor
        }

        val sat = saturation(P(r, g, b))
        if (sat < minSaturation) {
            val avg = (r + g + b) / 3f
            r = avg + (r - avg) * 1.3f
            g = avg + (g - avg) * 1.3f
            b = avg + (b - avg) * 1.3f
        }

        return P(
            r.coerceIn(0f, 255f),
            g.coerceIn(0f, 255f),
            b.coerceIn(0f, 255f)
        )
    }

    fun toColorInt(p: P): Int =
        (0xFF shl 24) or
                (p.r.toInt() shl 16) or
                (p.g.toInt() shl 8) or
                p.b.toInt()

    val original = ImageIO.read(File(path))
    val scaled = original.getScaledInstance(targetSize, targetSize, Image.SCALE_FAST)

    val img = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB)
    img.createGraphics().apply {
        drawImage(scaled, 0, 0, null)
        dispose()
    }

    val points = ArrayList<P>(targetSize * targetSize / 4)

    for (x in 0 until targetSize step 2) {
        for (y in 0 until targetSize step 2) {
            val rgb = img.getRGB(x, y)
            val r = ((rgb shr 16) and 0xFF).toFloat()
            val g = ((rgb shr 8) and 0xFF).toFloat()
            val b = (rgb and 0xFF).toFloat()

            val p = P(r, g, b)

            // фильтрация мусора
            if (brightness(p) < 0.1f) continue
            if (saturation(p) < 0.1f) continue

            points += p
        }
    }

    if (points.isEmpty()) return 0xFF444444.toInt()

    val centers = points
        .groupBy { ((it.r + it.g + it.b) / 3f / 256f * clustersCount).toInt() }
        .values
        .take(clustersCount)
        .map { cluster ->
            val r = cluster.sumOf { it.r.toDouble() } / cluster.size
            val g = cluster.sumOf { it.g.toDouble() } / cluster.size
            val b = cluster.sumOf { it.b.toDouble() } / cluster.size
            P(r.toFloat(), g.toFloat(), b.toFloat())
        }
        .toMutableList()

    val buckets = Array(centers.size) { mutableListOf<P>() }

    repeat(8) {
        buckets.forEach { it.clear() }

        for (p in points) {
            val idx = centers.indices.minBy { i ->
                val c = centers[i]
                val dr = p.r - c.r
                val dg = p.g - c.g
                val db = p.b - c.b
                dr * dr + dg * dg + db * db
            }
            buckets[idx].add(p)
        }

        for (i in centers.indices) {
            val bucket = buckets[i]
            if (bucket.isEmpty()) continue

            val r = bucket.sumOf { it.r.toDouble() } / bucket.size
            val g = bucket.sumOf { it.g.toDouble() } / bucket.size
            val b = bucket.sumOf { it.b.toDouble() } / bucket.size
            centers[i] = P(r.toFloat(), g.toFloat(), b.toFloat())
        }
    }


    fun clampBrightness(
        c: P,
        minBrightness: Float = 0.35f
    ): P {
        val max = maxOf(c.r, c.g, c.b)
        if (max <= 0f) return c

        val target = minBrightness * 255f

        if (max >= target) return c

        val scale = target / max

        return P(
            (c.r * scale).coerceIn(0f, 255f),
            (c.g * scale).coerceIn(0f, 255f),
            (c.b * scale).coerceIn(0f, 255f)
        )
    }


    val dominant = centers.indices
        .maxBy { i ->
            val c = centers[i]
            buckets[i].size *
                    saturation(c) *
                    brightness(c)
        }
        .let { centers[it] }

    val bright = clampBrightness(dominant, minBrightness = 0.85f)
    val vivid  = clampSaturation(bright, minSaturation = 0.35f)
    return toColorInt(vivid)

}

fun pickFolderKDialog(): File? {
    return try {
        val process = ProcessBuilder(
            "kdialog",
            "--getexistingdirectory"
        ).start()

        val result = process.inputStream
            .bufferedReader()
            .readText()
            .trim()

        if (result.isNotEmpty()) File(result) else null
    } catch (e: Exception) {
        null
    }
}

fun pickFolderZenity(): File? {
    return try {
        val process = ProcessBuilder(
            "zenity",
            "--file-selection",
            "--directory"
        ).start()

        val result = process.inputStream
            .bufferedReader()
            .readText()
            .trim()

        if (result.isNotEmpty()) File(result) else null
    } catch (e: Exception) {
        null
    }
}

fun pickFolderNative(): File? {
    val dialog = FileDialog(Frame(), "Выберите папку")
    dialog.isVisible = true

    val dir = dialog.directory ?: return null
    return File(dir)
}

fun pickFolderLinuxNative(): File? {
    return pickFolderKDialog()
        ?: pickFolderZenity()
        ?: pickFolderNative()
}

object OS {

    val os = System.getProperty("os.name").lowercase()

    val isWindows = os.contains("win")
    val isLinux   = os.contains("linux")

    val arch = System.getProperty("os.arch")

    fun libExt(): String = when {
        isWindows -> ".dll"
        isLinux -> ".so"
        else -> error("Unsupported OS")
    }

}

fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 6.dp,
    shape: Shape = RectangleShape
) = this.drawBehind {

    val strokeWidth = width.toPx()

    val dashPx = dashLength.toPx()
    val gapPx = gapLength.toPx()

    val paint = Paint().apply {
        this.color = color
        style = PaintingStyle.Stroke
        this.strokeWidth = strokeWidth

        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashPx, gapPx),
            0f
        )
    }

    val outline = shape.createOutline(size, layoutDirection, this)

    when (outline) {

        is Outline.Rectangle -> {
            drawRect(
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dashPx, gapPx),
                        0f
                    )
                )
            )
        }

        is Outline.Rounded -> {
            drawRoundRect(
                color = color,
                cornerRadius = outline.roundRect.topLeftCornerRadius,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dashPx, gapPx),
                        0f
                    )
                )
            )
        }

        is Outline.Generic -> {
            drawPath(
                outline.path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dashPx, gapPx),
                        0f
                    )
                )
            )
        }
    }
}

fun Modifier.bottomGradient(col: Color) = this.drawWithCache {
    val gradient = Brush.radialGradient(
        colors = listOf(
            col.copy(alpha = 0.10f),
            Color.Transparent
        ),
        center = Offset(size.width / 2f, size.height),
        radius = size.width * 0.5f
    )

    val strokeWidth = 1.dp.toPx()
    val y = strokeWidth / 2

    onDrawBehind {
        drawRect(gradient)
    }
}

fun openFileInFileManager(path : Path)
{
    val file = path.toFile()
    val os = System.getProperty("os.name").lowercase()

    try {
        when {
            // Windows: открывает проводник и выделяет файл
            os.contains("win") -> {
                Runtime.getRuntime().exec("explorer.exe /select,\"${file.absolutePath}\"")
            }

            // macOS: открывает Finder и выделяет файл (-R = reveal)
            os.contains("mac") -> {
                Runtime.getRuntime().exec(arrayOf("open", "-R", file.absolutePath))
            }

            // Linux: открывает родительскую папку (выделение файла зависит от менеджера, обычно просто папка)
            os.contains("nix") || os.contains("nux") -> {

                val absolutePath = file.absolutePath
                val parentPath = file.parentFile?.absolutePath ?: absolutePath

                fun run(vararg cmd: String): Boolean {
                    return try {
                        ProcessBuilder(*cmd)
                            .redirectErrorStream(true)
                            .start()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (run("nautilus", "--select", absolutePath)) return
                if (run("dolphin", "--select", absolutePath)) return
                if (run("nemo", absolutePath)) return
                if (run("thunar", absolutePath)) return

                run("xdg-open", parentPath)
            }

            // Фоллбэк на обычное открытие, если ОС не распознана
            else -> {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file.parentFile ?: file)
                }
            }

        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

