package com.bsbeetle.bananaspaghetti.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.viewmodel.KeyGesture


@Composable
fun RowScope.KeyboardKey(
    key: Key,
    isShiftOn: Boolean,
    isCapsLockOn: Boolean,
    onKeyEvent: (Key, KeyGesture) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1.0f, label = "")


// Only highlight Shift key when either Shift or CapsLock is on
    val isShiftLit = (key.type == KeyType.ACTION_SHIFT && (isShiftOn || isCapsLockOn))


    val textToShow = when {
        isShiftOn or isCapsLockOn -> key.shiftText ?: key.text.uppercase()
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
//    val backgroundColor = if (isPressed) baseColor.copy(alpha = 0.85f) else baseColor
    val backgroundColor = when {
        isShiftLit -> Color(0xFF3A5FCD) // blue highlight when active
        isPressed -> baseColor.copy(alpha = 0.85f)
        else -> baseColor
    }

    val gestureModifier = when (key.type) {
        // The Shift key is special key
        // Press is buggy so commented out
        KeyType.ACTION_SHIFT -> Modifier.pointerInput(key) {
            detectTapGestures(
                onDoubleTap = { onKeyEvent(key, KeyGesture.DoubleTap) },
                onTap = { onKeyEvent(key, KeyGesture.Tap) }
            )
        }
//                onPress = {
//                    onKeyEvent(key, KeyGesture.Press)
//                    tryAwaitRelease()
//                    onKeyEvent(key, KeyGesture.Release)
//                },

        // All other keys get the fast, continuous-press treatment
        else -> Modifier.continuousPress { gesture ->
            onKeyEvent(key, gesture)
        }
    }

    Surface(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .weight(key.weight)
            .padding(1.dp)
            .fillMaxHeight()
            .then(gestureModifier),
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, Color(0xFF3A3A3C)),
        shadowElevation = if (isPressed) 1.dp else 3.dp
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
