package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun TopRightBox(modifier: Modifier = Modifier) {

    ConstraintLayout(modifier = modifier) {
        val (rightBar, leftSpace) = createRefs()

        // Left bar
        Box(
            modifier = Modifier
                .constrainAs(leftSpace) {
                    start.linkTo(parent.start)
                    end.linkTo(rightBar.start)
                    top.linkTo(parent.top)
                    width = Dimension.fillToConstraints
                }
                .fillMaxHeight()
                .background(Color(0xFF252525))
        ) {  }

        // Right Space
        Box(
            modifier = Modifier
                .constrainAs(rightBar) {
                    start.linkTo(leftSpace.end)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    width = Dimension.value(20.dp)
                }
                .fillMaxHeight()
                .background(Color(0xFF212121))
        ) {  }
    }

}