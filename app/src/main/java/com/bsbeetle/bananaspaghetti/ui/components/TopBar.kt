package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.data.TopBarButtons

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    color: Color,
    rButtons: List<TopBarButtons>? = null
) {
    Box(
        modifier = modifier
            .background(color)
            .fillMaxWidth()
            .height(15.dp),
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (leftTB, middleTB, rightTB) = createRefs()

            // Left top bar
            Row(
                modifier = Modifier
                    .constrainAs(leftTB) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.percent(0.2f)
                        height = Dimension.fillToConstraints
                    }
                    .background(Color(0xFFDA0C0C)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display buttons with their functionalities
                Row (
                    horizontalArrangement = Arrangement.Center
                ) {
                    rButtons?.forEach { button ->
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

            // Middle top bar
            Row(
                modifier = Modifier
                    .constrainAs(middleTB) {
                        start.linkTo(leftTB.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.percent(0.6f)
                        height = Dimension.fillToConstraints
                    }
                    .background(Color(0xFF0CDA16)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display buttons with their functionalities
                Row (
                    horizontalArrangement = Arrangement.Center
                ) {
                    // File Buffers
                }
            }

            // Right top bar
            Row(
                modifier = Modifier
                    .constrainAs(rightTB) {
                        start.linkTo(middleTB.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.percent(0.2f)
                        height = Dimension.fillToConstraints
                    }
                    .background(Color(0xFF0C27DA)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display buttons with their functionalities
                Row (
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Other Buttons
                }
            }

        }
    }
}