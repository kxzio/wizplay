package org.example.ui.screens.leftPager.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.DiscFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.LineHeightStyle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.nio.file.Path
import javax.swing.GroupLayout

@Composable
fun artworkAsync(
    path: Path?,
    modifier: Modifier = Modifier
) {

    if (path == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Sharp.Album, "",
                modifier = Modifier.fillMaxSize(0.7f),
                tint = Color(255, 255, 255, 60)
            )
        }
        return
    }

    AsyncImage(
        model = path.toString(),
        contentDescription = "Album Artwork",
        modifier = modifier,
        filterQuality = FilterQuality.Medium,
        contentScale = ContentScale.Crop
    )
}