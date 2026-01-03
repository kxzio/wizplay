package ui.screens.rightPager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.wizui.wizui

@Composable
fun renderRightPager()
{
    wizui.wizColumn(modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth()
        .background(Color(10, 10, 10))
        .padding(16.dp)
    )
    {

    }
}