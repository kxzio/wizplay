package ui.screens.rightPager.tracklist.track.dropdownAndPopups

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Create
import androidx.compose.material.icons.sharp.LastPage
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
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
import org.example.audioindex.ScannedAudio
import org.example.folderGetter.Playlist
import org.example.folderGetter.PlaylistController
import org.example.ui.uiHelpers.AniJinPopup
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf



@Composable
fun handleTracksPopUp(
    handleTargetTrack : MutableState<ScannedAudio?>,
    playlistController: PlaylistController
)
{

    val playlists by playlistController.playlists.collectAsState()

    //playlist add
    AniJinPopup(
        focusable = true,
        expanded = handleTargetTrack.value != null,
        onDismissRequest =
            {
                handleTargetTrack.value = null
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
                    modifier = Modifier.padding(16.dp).align(Alignment.Center)
                ) {

                    Column(Modifier.padding(32.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            IconButton( {
                                handleTargetTrack.value = null
                            }){
                                Icon(Icons.Sharp.Close, "", tint = Color.White)
                            }

                            Text("select playlist to add..", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))

                        }

                        LazyColumn(Modifier.padding(top = 16.dp).width(400.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                            items(playlists) {

                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                                        .clickable {
                                            playlistController.addTrack(it.id, handleTargetTrack.value!!)
                                            handleTargetTrack.value = null
                                        }
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .size(75.dp)
                                            .aspectRatio(1f)
                                            .background(Color(45, 45, 45))

                                    ) {

                                    }

                                    Text(it.name, color = Color(255, 255, 255), modifier = Modifier.padding(horizontal = 16.dp))

                                }

                            }

                        }




                    }
                }
            }
        }
    )

}
