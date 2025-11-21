package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val backgroundColor = Color(0xFFFFFFFF)

    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Same backdrop effect as buttons
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(8.dp) },
                    effects = {
                        vibrancy()
                        blur(15f.dp.toPx())
                        lens(25f.dp.toPx(), 50f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.01f)) // Same as buttons
                    }
                )
        )

        // Glass edge border
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 0.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x80FFFFFF),
                            Color(0x40FFFFFF),
                            Color(0x80FFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        // Content
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(24.dp),
            content = content
        )
    }
}