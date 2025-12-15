package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun TopRightBox(
    modifier: Modifier = Modifier,
    isVisible: Boolean = false
) {
    ConstraintLayout(
        modifier = modifier
            .background(Color(0xFF050505))
    ) {
        val (gridContent) = createRefs()

        if (isVisible) {
            Column(
                modifier = Modifier
                    .constrainAs(gridContent) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)

                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "<Branch name>",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Row 1
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD9D9D9)))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD9D9D9)))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD9D9D9)))
                }

                // Row 2
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD9D9D9)))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD9D9D9)))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD9D9D9)))
                }

                // Row 3
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1.2f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1.5f).fillMaxHeight().background(Color(0xFFD9D9D9)))
                    Box(Modifier.weight(1.5f).fillMaxHeight().background(Color(0xFFD9D9D9)))
                }
            }
        }
    }
}
