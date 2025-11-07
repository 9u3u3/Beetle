package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun VignetteEffect(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    intensity: Float = 0.7f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gradient = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = intensity)
            ),
            center = center,
            radius = size.maxDimension * 0.8f
        )

        drawRect(brush = gradient)
    }
}