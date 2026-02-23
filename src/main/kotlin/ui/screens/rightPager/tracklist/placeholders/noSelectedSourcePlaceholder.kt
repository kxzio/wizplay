package org.example.ui.screens.rightPager.tracklist.placeholders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PermMedia
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun notSelectedSourcePlaceholder()
{
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {

        Icon(
            Icons.Sharp.PermMedia, "",
            tint = Color(255, 255, 255, 30),
            modifier = Modifier.size(150.dp)
        )

        Text("select album or playlist from the media-tab", fontSize = 16.sp, color = Color(255, 255, 255))
    }
}