package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.viewmodel.EditorAction
import com.bsbeetle.bananaspaghetti.viewmodel.KeyGesture
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@Composable
fun ExtraKeysRow(
    keyboardViewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Row 1 keys
    val row1 = listOf(
        "ESC" to KeyType.ACTION_ESCAPE,
        "/" to KeyType.CHARACTER,
        "-" to KeyType.CHARACTER,
        "HOME" to KeyType.ACTION_CTRL, // Placeholder type, handled manually
        "UP" to KeyType.ACTION_CTRL,
        "END" to KeyType.ACTION_CTRL,
        "PGUP" to KeyType.ACTION_CTRL
    )

    // Row 2 keys
    val row2 = listOf(
        "TAB" to KeyType.ACTION_TAB,
        "CTRL" to KeyType.ACTION_CTRL,
        "ALT" to KeyType.ACTION_CTRL, // Will add ALT type later
        "LEFT" to KeyType.ACTION_CTRL,
        "DOWN" to KeyType.ACTION_CTRL,
        "RIGHT" to KeyType.ACTION_CTRL,
        "PGDN" to KeyType.ACTION_CTRL
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2E2E2E))
            .padding(vertical = 4.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row1.forEach { (label, type) ->
                ExtraKeyButton(
                    label = label,
                    isActive = false, // No toggle state for these yet
                    onClick = {
                        handleExtraKey(label, keyboardViewModel)
                    }
                )
            }
        }
        
        // Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState) // Share scroll state? Or independent? Independent is better for now.
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row2.forEach { (label, type) ->
                val isActive = when (label) {
                    "CTRL" -> uiState.isCtrlOn
                    "ALT" -> uiState.isAltOn
                    else -> false
                }
                ExtraKeyButton(
                    label = label,
                    isActive = isActive,
                    onClick = {
                        handleExtraKey(label, keyboardViewModel)
                    }
                )
            }
        }
    }
}

@Composable
fun ExtraKeyButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFF228edd) else Color(0xFF424242))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun handleExtraKey(label: String, viewModel: KeyboardViewModel) {
    when (label) {
        "ESC" -> viewModel.onKeyEvent(Key("ESC", type = KeyType.ACTION_ESCAPE), KeyGesture.Tap)
        "/" -> viewModel.onKeyEvent(Key("/", type = KeyType.CHARACTER), KeyGesture.Tap)
        "-" -> viewModel.onKeyEvent(Key("-", type = KeyType.CHARACTER), KeyGesture.Tap)
        "TAB" -> viewModel.onKeyEvent(Key("TAB", type = KeyType.ACTION_TAB), KeyGesture.Tap)
        "CTRL" -> viewModel.onKeyEvent(Key("CTRL", type = KeyType.ACTION_CTRL), KeyGesture.Tap)
        "ALT" -> viewModel.onKeyEvent(Key("ALT", type = KeyType.ACTION_ALT), KeyGesture.Tap)
        
        // Special sequences for Terminal
        "HOME" -> sendSequence(viewModel, "\u001B[H")
        "END" -> sendSequence(viewModel, "\u001B[F")
        "PGUP" -> sendSequence(viewModel, "\u001B[5~")
        "PGDN" -> sendSequence(viewModel, "\u001B[6~")
        "UP" -> sendSequence(viewModel, "\u001B[A")
        "DOWN" -> sendSequence(viewModel, "\u001B[B")
        "RIGHT" -> sendSequence(viewModel, "\u001B[C")
        "LEFT" -> sendSequence(viewModel, "\u001B[D")
    }
}

fun sendSequence(viewModel: KeyboardViewModel, sequence: String) {
    // We need to expose a way to send raw sequence or use emitText
    // Since emitText is private, we might need to use a public method or add one.
    // For now, let's assume we can add a method to ViewModel or use a workaround.
    // Actually, we can add a public method `sendTerminalSequence` to KeyboardViewModel.
    viewModel.sendTerminalSequence(sequence)
}
