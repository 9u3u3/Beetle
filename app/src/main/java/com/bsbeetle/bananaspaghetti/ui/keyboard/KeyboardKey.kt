package com.bsbeetle.bananaspaghetti.ui.keyboard

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.ui.theme.codaRegular
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

    val textToShow = when {
        isShiftOn or isCapsLockOn -> key.shiftText ?: key.text.uppercase()
        else -> key.text
    }

    val gestureModifier = when (key.type) {
        KeyType.ACTION_CTRL -> Modifier.pointerInput(key) {
            detectTapGestures(
                onTap = { onKeyEvent(key, KeyGesture.Tap) }
            )
        }
        KeyType.ACTION_SHIFT -> Modifier.pointerInput(key) {
            detectTapGestures(
                onDoubleTap = { onKeyEvent(key, KeyGesture.DoubleTap) },
                onTap = { onKeyEvent(key, KeyGesture.Tap) }
            )
        }
        else -> Modifier.continuousPress(key = key) { gesture -> // Pass the key object here
            onKeyEvent(key, gesture)
        }
    }

    Surface(
        modifier = modifier
            .weight(key.weight)
            .fillMaxHeight()
            .then(gestureModifier),
         color = Color(0xFF212121),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = textToShow,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPressed) Color(0xFFB0C4DE) else Color.White,
                fontFamily = codaRegular
            )
        }
    }
}