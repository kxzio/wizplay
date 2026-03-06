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
import androidx.compose.ui.geometry.Rect
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

fun bakeArtworkGrid(images: List<ImageBitmap>, size: Int = 500): ImageBitmap {

    val result = ImageBitmap(size, size)
    val canvas = Canvas(result)

    val paint = Paint()

    fun drawFit(
        canvas: Canvas,
        img: ImageBitmap,
        dstX: Int,
        dstY: Int,
        dstW: Int,
        dstH: Int,
        paint: Paint
    ) {

        val scale = minOf(
            dstW.toFloat() / img.width,
            dstH.toFloat() / img.height
        )

        val w = (img.width * scale).toInt()
        val h = (img.height * scale).toInt()

        val x = dstX + (dstW - w) / 2
        val y = dstY + (dstH - h) / 2

        canvas.drawImageRect(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset(x, y),
            dstSize = IntSize(w, h),
            paint = paint
        )
    }

    fun drawCrop(
        canvas: Canvas,
        img: ImageBitmap,
        dstX: Int,
        dstY: Int,
        dstW: Int,
        dstH: Int,
        paint: Paint
    ) {

        val srcRatio = img.width.toFloat() / img.height
        val dstRatio = dstW.toFloat() / dstH

        val srcX: Int
        val srcY: Int
        val srcW: Int
        val srcH: Int

        if (srcRatio > dstRatio) {
            // шире → режем по ширине (центр)
            srcH = img.height
            srcW = (srcH * dstRatio).toInt()
            srcX = (img.width - srcW) / 2
            srcY = 0
        } else {
            // выше → режем по высоте (прижимаем вверх)
            srcW = img.width
            srcH = (srcW / dstRatio).toInt()
            srcX = 0
            srcY = 0
        }

        canvas.drawImageRect(
            image = img,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW, srcH),
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(dstW, dstH),
            paint = paint
        )
    }
    
    val half = size / 2

    when (images.size) {

        1 -> {
            drawCrop(canvas, images[0], 0, 0, size, size, paint)
        }

        2 -> {
            drawCrop(canvas, images[0], 0, 0, half, size, paint)
            drawCrop(canvas, images[1], half, 0, half, size, paint)
        }

        3 -> {
            drawCrop(canvas, images[0], 0, 0, half, half, paint)
            drawCrop(canvas, images[1], half, 0, half, half, paint)
            drawCrop(canvas, images[2], 0, half, size, half, paint)
        }

        else -> {
            drawCrop(canvas, images[0], 0, 0, half, half, paint)
            drawCrop(canvas, images[1], half, 0, half, half, paint)
            drawCrop(canvas, images[2], 0, half, half, half, paint)
            drawCrop(canvas, images[3], half, half, half, half, paint)
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
                .size(500)
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