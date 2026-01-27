package org.example.ui.screens.leftPager.albums

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.DiscFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
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
    placeholderSize: Float = 0.7f
) {
    if (path == null) {
        Box(modifier.fillMaxSize() , contentAlignment = Alignment.Center) {
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

    val painter: AsyncImagePainter = rememberAsyncImagePainter(
        model = remember(path) {
            ImageRequest.Builder(context)
                .data(path.toString())
                .size(Size(512, 512))
                .memoryCacheKey(path.toString())
                .diskCacheKey(path.toString())
                .crossfade(durationMillis = 200)
                .build()
        },
        filterQuality = FilterQuality.Low,
        contentScale = ContentScale.Crop,
    )

    Box(modifier.background(Color(45, 45, 45))) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
