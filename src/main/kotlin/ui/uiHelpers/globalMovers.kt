package org.example.ui.uiHelpers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

//pending navigation
class globalUIMovers {
    var albumListMoveToAlbumKey by mutableStateOf<String?>(null)
    var playlistListMoveToPlaylistKey by mutableStateOf<String?>(null)
}

val wizuiUIMove = globalUIMovers()
