package org.example.ui.screens.leftPager.albums

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.DiscFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.request.transformations
import coil3.size.Size
import coil3.toBitmap
import org.example.audioindex.ScannedAudio
import org.example.ui.effects.PreRenderBlurTransformation
import java.nio.file.Path
import javax.swing.GroupLayout

@Composable
private fun dpToPx(dp: Dp): Int {
    val density = LocalDensity.current
    return with(density) { dp.roundToPx() }
}

@Composable
fun artworkAsync(
    path: Path?,
    modifier: Modifier = Modifier,
    placeholderSize: Float = 0.7f,
    blurRadius: Float = 0f
) {

    if (path == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Sharp.Album,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(placeholderSize),
                tint = Color(255, 255, 255, 60)
            )
        }
        return
    }

    val context = LocalPlatformContext.current

    val request = remember(path, blurRadius) {

        ImageRequest.Builder(context)
            .data(path.toString())

            // critical for caching
            .memoryCacheKey("artwork_${path}_blur_$blurRadius")
            .diskCacheKey("artwork_${path}_blur_$blurRadius")

            .crossfade(200)

            .apply {
                if (blurRadius > 0f) {
                    transformations(
                        PreRenderBlurTransformation(blurRadius)
                    )
                }
            }

            .build()
    }

    Box(
        modifier = modifier.background(Color(45, 45, 45))
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
    }
}

fun bakeArtworkGrid(
    bitmaps: List<ImageBitmap>,
    size: Int = 256
): ImageBitmap {

    val result = ImageBitmap(size, size)
    val canvas = Canvas(result)
    val paint = Paint()

    val count = bitmaps.size.coerceAtMost(4)
    val half = size / 2

    fun drawCrop(bmp: ImageBitmap, x: Int, y: Int) {

        val srcSize = minOf(bmp.width, bmp.height)

        canvas.drawImageRect(
            bmp,
            IntOffset(
                (bmp.width - srcSize) / 2,
                (bmp.height - srcSize) / 2
            ),
            IntSize(srcSize, srcSize),   // квадратный кроп
            IntOffset(x, y),
            IntSize(srcSize, srcSize),   // 1:1 без изменения
            paint
        )
    }

    when (count) {

        1 -> {
            drawCrop(bitmaps[0], 0, 0)
        }

        2 -> {
            drawCrop(bitmaps[0], 0, 0)
            drawCrop(bitmaps[1], 0, half)
        }

        3 -> {
            drawCrop(bitmaps[0], 0, 0)
            drawCrop(bitmaps[1], half, 0)
            drawCrop(bitmaps[2], 0, half)
        }

        else -> {
            bitmaps.take(4).forEachIndexed { i, bmp ->
                val x = (i % 2) * half
                val y = (i / 2) * half
                drawCrop(bmp, x, y)
            }
        }
    }

    return result
}

@Composable
fun drawAudioSourceArtwork(
    pathes: List<Path?>,
    modifier: Modifier = Modifier
) {

    if (pathes.isEmpty()) return

    if (pathes.size == 1) {
        artworkAsync(pathes.first(), modifier)
        return
    }

    val context = LocalPlatformContext.current
    val imageLoader = remember { ImageLoader(context) }

    val bitmaps = remember { mutableStateListOf<ImageBitmap>() }

    LaunchedEffect(pathes) {

        bitmaps.clear()

        pathes.take(4).forEach { path ->

            if (path == null) return@forEach

            val request = ImageRequest.Builder(context)
                .data(path.toString())
                .size(256)
                .crossfade(false)
                .build()

            val result = imageLoader.execute(request)

            if (result is SuccessResult) {

                val image = result.image

                val bmp = image.toBitmap()

                bitmaps.add(bmp.asComposeImageBitmap())
            }
        }
    }

    val baked = remember(bitmaps.size) {
        if (bitmaps.isNotEmpty())
            bakeArtworkGrid(bitmaps)
        else null
    }

    baked?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}