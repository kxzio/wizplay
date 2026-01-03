package ui.screens.leftPager.albums

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBackIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.FullscreenController
import org.example.wizui.wizui
import org.example.wizui.wizui.wizAnimateIf

@Composable
fun albumTab(
    openedTab: MutableState<Int>,
)
{
    var searchQr by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    (openedTab.value == 2).wizAnimateIf(wizui.WizAnimationType.ExpandVertically) {
        Column(Modifier.padding(top = 8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                wizui.wizBlinkingText(
                    "albums",
                    normalColor = Color(255, 255, 255),
                    blinkColor = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 12.dp),
                    onClick = {

                    }
                )

            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                thickness = 1.0.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Row {
                BasicTextField(
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    value = searchQr,
                    onValueChange = { searchQr = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .border(
                            width = 0.5.dp,
                            color = if (searchQr.isNotEmpty() || isFocused)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            shape = RectangleShape
                        )
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->

                        (searchQr.isEmpty() && !isFocused).wizAnimateIf {
                            Text(
                                "searching",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        innerTextField()
                    }
                )

                (!searchQr.isEmpty()).wizAnimateIf(wizui.WizAnimationType.ExpandHorizontally) {
                    Button({
                        searchQr = ""
                    },
                        modifier = Modifier.height(40.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color(255, 255, 255),
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    {
                        Text("clear")
                    }
                }

            }

        }
    }

}