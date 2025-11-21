package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.utils.fileHandling.Tree

@Composable
fun TopLeftBox(
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier.background(Color(0xFF050505))) {
        val (rightSpace) = createRefs()
        Tree(
            modifier = Modifier
                .constrainAs(rightSpace) {
                 start.linkTo(parent.start)
                 end.linkTo(parent.end)
                 top.linkTo(parent.top)
                 width = Dimension.fillToConstraints
                }
                .graphicsLayer(
                    scaleX = 0.9f,
                    scaleY = 0.9f,
                    translationX = -40f,
                    translationY = -25f
                )
                .fillMaxSize()
        )
    }
}
@Composable
fun TopLeftTermBox(
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier.background(Color(0xFF050505))) {
    }
}
