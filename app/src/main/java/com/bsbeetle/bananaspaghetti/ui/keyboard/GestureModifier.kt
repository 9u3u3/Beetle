package com.bsbeetle.bananaspaghetti.ui.keyboard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.bsbeetle.bananaspaghetti.viewmodel.KeyGesture
import kotlinx.coroutines.*

private const val CONTINUOUS_PRESS_INITIAL_DELAY_MS: Long = 400  // Reduced from 500ms for faster response
private const val CONTINUOUS_PRESS_REPEAT_INTERVAL_MS: Long = 150 // Increased from 100ms to reduce pressure on WebView

fun Modifier.continuousPress(
    key: Any?,
    onGesture: (KeyGesture) -> Unit
): Modifier = this.pointerInput(key) { // Use that key here instead of Unit
    detectTapGestures(
        onPress = {
            // Send immediate press
            onGesture(KeyGesture.Press)

            // Use coroutineScope so we can safely launch a child coroutine
            coroutineScope {
                var isLongPressTriggered = false
                // After 500ms → LongPress fires repeatedly every 100ms
                val job = launch {
                    delay(CONTINUOUS_PRESS_INITIAL_DELAY_MS)
                    isLongPressTriggered = true
                    while (isActive) {
                        onGesture(KeyGesture.LongPress)
                        delay(CONTINUOUS_PRESS_REPEAT_INTERVAL_MS)
                    }
                }

                // Wait until finger is released
                val released = tryAwaitRelease()

                // Cancel repeating job
                job.cancel()

                if (released && !isLongPressTriggered) {
                    // If released before threshold → treat as tap
                    onGesture(KeyGesture.Tap)
                }

                // Always send release
                onGesture(KeyGesture.Release)
            }
        }
    )
}