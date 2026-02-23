package org.example.ui.screens.rightPager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.DeleteForever
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun createDropDownTrack(
    offsetFromIntToDp : DpOffset,
    expanded : Boolean,
    onDismissRequest: () -> Unit,
)
{
    DropdownMenu(
        shape = RectangleShape,
        offset = offsetFromIntToDp,
        containerColor = Color(20, 20, 20),
        border = BorderStroke(0.5.dp, Color(255, 255, 255, 50)),
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(220.dp).padding(horizontal = 8.dp)
    ) {

        DropdownMenuItem(
            text = { Text("open") },
            onClick = {
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = { Text("rename") },
            onClick = {
                onDismissRequest()
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DropdownMenuItem(
            text = {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(Icons.Sharp.DeleteForever, "",
                        tint = Color(226, 80, 80, 255))

                    Text(
                        "delete",
                        modifier = Modifier.padding(start = 12.dp),
                        color = Color(226, 80, 80, 255)
                    )
                }
            },
            onClick = {
                onDismissRequest()
            }
        )
    }
}