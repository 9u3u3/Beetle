package com.bsbeetle.bananaspaghetti.ui.keyboard


import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType

@Composable
fun RowScope.KeyboardKey(
    key: Key,
    isCapsLockOn: Boolean,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate scale
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "keyScaleAnimation"
    )

    val textToShow = when {
        isCapsLockOn -> key.shiftText ?: key.text.uppercase()
        else -> key.text
    }

    // Base colors per key type (dark theme palette)
    val baseColor = when (key.type) {
        KeyType.CHARACTER -> Color(0xFF1C1C1E) // dark charcoal
        KeyType.ACTION_SHIFT,
        KeyType.ACTION_ENTER,
        KeyType.ACTION_BACKSPACE -> Color(0xFF2C2C30) // slightly lighter gray
        else -> Color(0xFF050A30) // deep accent blue
    }

    // Pressed → slightly darker
    val backgroundColor = if (isPressed) baseColor.copy(alpha = 0.85f) else baseColor

    Surface(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale) // keep press animation
            .weight(key.weight)
            .padding(1.dp)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onKeyPress(textToShow) }
            ),
        color = backgroundColor,
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.dp,
            if (isCapsLockOn && key.text=="⇧")
                Color(0xFF00E5FF) // teal glow for active Caps
            else Color(0xFF2E2E38)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = textToShow,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPressed) Color(0xFFB0C4DE) else Color.White
            )
        }
    }
}
