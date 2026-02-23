package ui.screens.leftPager.audioSources.playlists.popUps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Create
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.example.folderGetter.Playlist
import org.example.folderGetter.PlaylistController
import org.example.ui.uiHelpers.AniJinPopup
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf

@Composable
fun handlePlaylistPopUps(
    playlistCreateWindow: Boolean,
    onPlaylistCreateWindowChange: (Boolean) -> Unit,

    newPlaylistName: String,
    onNewPlaylistNameChange: (String) -> Unit,

    deletePlaylistId: MutableState<String>,
    renamePlaylistId: MutableState<String>,
    renamePlaylistString: MutableState<String>,
    highlitedPlaylist: MutableState<String>,

    audioMap: List<Playlist>,
    playlistController: PlaylistController,

    primary: Color
)
{
    val createPlaylistErrorText = remember { mutableStateOf("") }
    val renamePlaylistErrorText = remember { mutableStateOf("") }
    val errorWindowDragging     = remember { Animatable(0f) }

    //error animation
    LaunchedEffect(createPlaylistErrorText.value, renamePlaylistErrorText.value)
    {

        if (createPlaylistErrorText.value.isEmpty() && renamePlaylistErrorText.value.isEmpty())
            return@LaunchedEffect

        errorWindowDragging.snapTo(0f)

        errorWindowDragging.animateTo(
            2f,
            tween(100, easing = FastOutLinearInEasing)
        )

        errorWindowDragging.animateTo(
            -2f,
            tween(100, easing = LinearOutSlowInEasing)
        )

        errorWindowDragging.animateTo(
            1.5f,
            tween(100, easing = FastOutLinearInEasing)
        )

        errorWindowDragging.animateTo(
            -1.5f,
            tween(100, easing = LinearOutSlowInEasing)
        )

        errorWindowDragging.animateTo(
            1f,
            tween(100, easing = FastOutLinearInEasing)
        )

        errorWindowDragging.animateTo(
            -1f,
            tween(100, easing = FastOutLinearInEasing)
        )

        errorWindowDragging.animateTo(
            0.5f,
            tween(100, easing = FastOutLinearInEasing)
        )

        errorWindowDragging.animateTo(
            -0.5f,
            tween(100, easing = FastOutLinearInEasing)
        )


        errorWindowDragging.animateTo(
            0f,
            tween(100, easing = LinearOutSlowInEasing)
        )

    }

    //playlist add
    AniJinPopup(
        focusable = true,
        expanded = playlistCreateWindow,
        onDismissRequest =
            {
                onPlaylistCreateWindowChange(false)
                onNewPlaylistNameChange("")
                createPlaylistErrorText.value = ""
            },
        enter =
            fadeIn(
                animationSpec = tween(120)
            ) +
                    scaleIn(
                        initialScale = 0.85f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
        exit =
            fadeOut(
                animationSpec = tween(90)
            ) +
                    scaleOut(
                        targetScale = 0.85f,
                        animationSpec = tween(120)
                    ),
        content = {
            Box(Modifier.fillMaxSize().background(Color(0, 0, 0, 100))) {
                Surface(
                    color = Color(20, 20, 20),
                    border = BorderStroke(0.5.dp, Color(255, 255, 255, 100)),
                    modifier = Modifier.padding(16.dp).align(Alignment.Center).offset(x = 3.dp * errorWindowDragging.value)
                ) {

                    Column(Modifier.padding(32.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            IconButton( {
                                onPlaylistCreateWindowChange(false)
                                onNewPlaylistNameChange("")
                                createPlaylistErrorText.value = ""
                            }){
                                Icon(Icons.Sharp.Close, "", tint = Color.White)
                            }

                            Text("create new playlist", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }


                        Spacer(Modifier.height(8.dp))
                        var isFocusedOnCreation by rememberSaveable { mutableStateOf(false) }
                        Row(Modifier.padding(top = 8.dp).zIndex(3f)) {
                            BasicTextField(
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                value = newPlaylistName,
                                onValueChange = {
                                    onNewPlaylistNameChange(it)
                                    createPlaylistErrorText.value = ""
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .width(400.dp)
                                    .height(40.dp)
                                    .background(Color(25, 25, 25, 150))
                                    .drawBehind {

                                        val y = size.height

                                        drawLine(
                                            if (isFocusedOnCreation) primary else Color(255, 255, 255, 100),
                                            Offset(0f, y),
                                            Offset(size.width, y),
                                            1f
                                        )
                                    }
                                    .onFocusChanged { focusState ->
                                        isFocusedOnCreation = focusState.isFocused
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (newPlaylistName.isEmpty() && !isFocusedOnCreation) {

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Sharp.Create, "", tint =
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                )
                                                Text(
                                                    "playlist name",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                    modifier = Modifier.padding(start = 16.dp)
                                                )
                                            }
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(400.dp)) {

                            wizui.wizButton(
                                backgroundColor = Color(0, 0, 0, 0),
                                modifier = Modifier.weight(1f).border(
                                    BorderStroke(0.5.dp, Color(255, 255, 255, 100))),
                                contentColor = Color.White,
                                shape = RectangleShape,
                                onClick = {
                                    onNewPlaylistNameChange("")
                                    createPlaylistErrorText.value = ""
                                    onPlaylistCreateWindowChange(false)
                                },
                            ){
                                Text("cancel")
                            }

                            (!newPlaylistName.isEmpty()).wizAnimateIf(wizui.WizAnimationType.ExpandHorizontally) {
                                wizui.wizButton(
                                    backgroundColor = Color(0, 0, 0, 0),
                                    modifier = Modifier.padding(start = 16.dp).weight(1f).border(
                                        BorderStroke(0.5.dp, primary)),
                                    contentColor = Color.White,
                                    shape = RectangleShape,
                                    onClick = {

                                        if (playlistController.playlists.value.any { it.name == newPlaylistName })
                                        {
                                            createPlaylistErrorText.value = "name of this playlist is already in use"
                                        }
                                        else if (newPlaylistName.isBlank())
                                        {
                                            createPlaylistErrorText.value = "name should consist of symbols/digits"
                                        }
                                        else
                                        {
                                            highlitedPlaylist.value = newPlaylistName
                                            onPlaylistCreateWindowChange(false)
                                            playlistController.create(newPlaylistName)
                                            onNewPlaylistNameChange("")
                                            createPlaylistErrorText.value = ""
                                        }

                                    }
                                ){
                                    Text("create")
                                }
                            }
                        }

                        if (!createPlaylistErrorText.value.isEmpty()) {

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {

                                Icon(
                                    Icons.Sharp.Warning, "", tint = Color(226, 80, 80, 255),
                                )

                                Text(createPlaylistErrorText.value, color = Color(226, 80, 80, 255), modifier = Modifier.padding(start = 16.dp))
                            }

                        }

                    }
                }
            }
        }
    )

    //delete playlist
    AniJinPopup(
        focusable = true,
        expanded = deletePlaylistId.value != "",
        onDismissRequest =
            {
                deletePlaylistId.value = ""
            },
        enter =
            fadeIn(
                animationSpec = tween(120)
            ) +
                    scaleIn(
                        initialScale = 0.85f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
        exit =
            fadeOut(
                animationSpec = tween(90)
            ) +
                    scaleOut(
                        targetScale = 0.85f,
                        animationSpec = tween(120)
                    ),
        content = {
            Box(Modifier.fillMaxSize().background(Color(0, 0, 0, 100))) {
                Surface(
                    color = Color(20, 20, 20),
                    border = BorderStroke(0.5.dp, Color(255, 255, 255, 100)),
                    modifier = Modifier.padding(16.dp).align(Alignment.Center).offset(x = 3.dp * errorWindowDragging.value).width(400.dp)
                ) {

                    Column(Modifier.padding(32.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            IconButton( {
                                deletePlaylistId.value = ""
                            }){
                                Icon(Icons.Sharp.Close, "", tint = Color.White)
                            }

                            val playlist = audioMap.firstOrNull {
                                it.id == deletePlaylistId.value.toLongOrNull()
                            }

                            Text(  "delete playlist ${playlist?.name ?: ""}?", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }


                        Spacer(Modifier.height(16   .dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.wrapContentWidth()
                            ) {

                                wizui.wizButton(
                                    backgroundColor = Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(BorderStroke(0.5.dp, Color(255, 255, 255, 100))),
                                    contentColor = Color.White,
                                    shape = RectangleShape,
                                    onClick = {
                                        deletePlaylistId.value = ""
                                    },
                                ) {
                                    Text("n (no)")
                                }

                                wizui.wizButton(
                                    backgroundColor = Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(BorderStroke(0.5.dp, primary)),
                                    contentColor = Color.White,
                                    shape = RectangleShape,
                                    onClick = {
                                        playlistController.delete(deletePlaylistId.value.toLong())
                                        deletePlaylistId.value = ""
                                    },
                                ) {
                                    Text("y (yes)")
                                }
                            }
                        }



                    }
                }
            }
        }
    )

    LaunchedEffect(renamePlaylistId.value) {
        renamePlaylistString.value = audioMap.firstOrNull {
            it.id == renamePlaylistId.value.toLongOrNull()
        }?.name ?: ""
    }

    //rename playlist
    AniJinPopup(
        focusable = true,
        expanded = renamePlaylistId.value != "",
        onDismissRequest =
            {
                renamePlaylistErrorText.value = ""
                renamePlaylistId.value = ""
            },
        enter =
            fadeIn(
                animationSpec = tween(120)
            ) +
                    scaleIn(
                        initialScale = 0.85f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
        exit =
            fadeOut(
                animationSpec = tween(90)
            ) +
                    scaleOut(
                        targetScale = 0.85f,
                        animationSpec = tween(120)
                    ),
        content = {
            Box(Modifier.fillMaxSize().background(Color(0, 0, 0, 100))) {
                Surface(
                    color = Color(20, 20, 20),
                    border = BorderStroke(0.5.dp, Color(255, 255, 255, 100)),
                    modifier = Modifier.padding(16.dp).align(Alignment.Center).offset(x = 3.dp * errorWindowDragging.value).width(400.dp)
                ) {

                    Column(Modifier.padding(32.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            IconButton( {
                                renamePlaylistErrorText.value = ""
                                renamePlaylistId.value = ""
                            }){
                                Icon(Icons.Sharp.Close, "", tint = Color.White)
                            }

                            val playlist = audioMap.firstOrNull {
                                it.id == renamePlaylistId.value.toLongOrNull()
                            }

                            Text("remame playlist ${playlist?.name ?: ""}", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }

                        Spacer(Modifier.height(16   .dp))

                        var isFocusedOnCreation by rememberSaveable { mutableStateOf(false) }
                        Row(Modifier.padding(top = 8.dp).zIndex(3f)) {
                            BasicTextField(
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                value = renamePlaylistString.value,
                                onValueChange = {
                                    renamePlaylistErrorText.value = ""
                                    renamePlaylistString.value = it
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .width(400.dp)
                                    .height(40.dp)
                                    .background(Color(25, 25, 25, 150))
                                    .drawBehind {

                                        val y = size.height

                                        drawLine(
                                            if (isFocusedOnCreation) primary else Color(255, 255, 255, 100),
                                            Offset(0f, y),
                                            Offset(size.width, y),
                                            1f
                                        )
                                    }
                                    .onFocusChanged { focusState ->
                                        isFocusedOnCreation = focusState.isFocused
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (renamePlaylistString.value.isEmpty() && !isFocusedOnCreation) {

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Sharp.Create, "", tint =
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                )
                                                Text(
                                                    "new playlist name",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                    modifier = Modifier.padding(start = 16.dp)
                                                )
                                            }
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(16   .dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.wrapContentWidth()
                            ) {

                                wizui.wizButton(
                                    backgroundColor = Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(BorderStroke(0.5.dp, Color(255, 255, 255, 100))),
                                    contentColor = Color.White,
                                    shape = RectangleShape,
                                    onClick = {
                                        renamePlaylistErrorText.value = ""
                                        renamePlaylistId.value = ""
                                    },
                                ) {
                                    Text("cancel")
                                }

                                wizui.wizButton(
                                    backgroundColor = Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(BorderStroke(0.5.dp, primary)),
                                    contentColor = Color.White,
                                    shape = RectangleShape,
                                    onClick = {

                                        if (playlistController.playlists.value.any { it.name == renamePlaylistString.value })
                                        {
                                            renamePlaylistErrorText.value = "name of this playlist is already in use"
                                        }
                                        else if (renamePlaylistString.value.isBlank())
                                        {
                                            renamePlaylistErrorText.value = "name should consist of symbols/digits"
                                        }
                                        else
                                        {
                                            renamePlaylistErrorText.value = ""
                                            playlistController.rename(id = renamePlaylistId.value.toLong(), renamePlaylistString.value)
                                            renamePlaylistId.value = ""
                                        }


                                    },
                                ) {
                                    Text("apply!")
                                }
                            }
                        }

                        if (!renamePlaylistErrorText.value.isEmpty()) {

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {

                                Icon(
                                    Icons.Sharp.Warning, "", tint = Color(226, 80, 80, 255),
                                )

                                Text(renamePlaylistErrorText.value, color = Color(226, 80, 80, 255), modifier = Modifier.padding(start = 16.dp))
                            }

                        }



                    }
                }
            }
        }
    )
}