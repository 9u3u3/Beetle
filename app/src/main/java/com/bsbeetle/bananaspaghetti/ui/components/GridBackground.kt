package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun GridBackground(
    modifier: Modifier = Modifier,
    gapSize: Int = 200
) {

    Canvas(
        modifier = modifier
    ) {
        for(i in 0..size.width.toInt() step gapSize) {
            if(i == 0) continue
            drawLine(
                color = Color(0xff262626),
                start = Offset(i.toFloat(), 0f),
                end = Offset(i.toFloat(), size.height),
                strokeWidth = 5f
            )
        }
        for(i in 0..size.height.toInt() step gapSize) {
            if(i == 0) continue
            drawLine(
                color = Color(0xff262626),
                start = Offset(0f, i.toFloat()),
                end = Offset(size.width, i.toFloat()),
                strokeWidth = 5f
            )
        }
    }
}