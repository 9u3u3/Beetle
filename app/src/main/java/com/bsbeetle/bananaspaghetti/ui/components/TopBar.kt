package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.data.TopBarButtons

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    color: Color,
    buttons: List<TopBarButtons>
) {
    Box(
        modifier = modifier
            .background(color)
            .fillMaxWidth()
            .height(15.dp),
    ) {
        // Here there will be a row of buttons
        Row(
            modifier = Modifier.padding(start = 30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display buttons with their functionalities
            Row (
                horizontalArrangement = Arrangement.Center
            ) {
                buttons.forEach { button ->
                    Box(
                        modifier = Modifier
                            .clickable{button.onClick()},
                    ) {
                        Text(
                            text = button.title,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight()
                            .width(1.dp),
                        color = Color(0xFF8D8D8D)
                    )
                }
            }
        }
    }
}