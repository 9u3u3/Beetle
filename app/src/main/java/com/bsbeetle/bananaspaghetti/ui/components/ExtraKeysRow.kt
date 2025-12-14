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
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.viewmodel.EditorAction
import com.bsbeetle.bananaspaghetti.viewmodel.KeyGesture
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType

@Composable
fun ExtraKeysRow(
    keyboardViewModel: KeyboardViewModel,
    mode: SessionType,
    modifier: Modifier = Modifier
) {
    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Editor Keys
    val editorRow1 = listOf(
        "ESC" to KeyType.ACTION_ESCAPE,
        "/" to KeyType.CHARACTER,
        "-" to KeyType.CHARACTER,
        "{" to KeyType.CHARACTER,
        "}" to KeyType.CHARACTER,
        "(" to KeyType.CHARACTER,
        ")" to KeyType.CHARACTER
    )

    val editorRow2 = listOf(
        "TAB" to KeyType.ACTION_TAB,
        "CTRL" to KeyType.ACTION_CTRL,
        "ALT" to KeyType.ACTION_ALT,
        "[" to KeyType.CHARACTER,
        "]" to KeyType.CHARACTER,
        "<" to KeyType.CHARACTER,
        ">" to KeyType.CHARACTER
    )

    // Terminal Keys
    val terminalRow1 = listOf(
        "ESC" to KeyType.ACTION_ESCAPE,
        "/" to KeyType.CHARACTER,
        "-" to KeyType.CHARACTER,
        "HOME" to KeyType.ACTION_HOME,
        "UP" to KeyType.ACTION_UP,
        "END" to KeyType.ACTION_END,
        "PGUP" to KeyType.ACTION_PGUP
    )

    val terminalRow2 = listOf(
        "TAB" to KeyType.ACTION_TAB,
        "CTRL" to KeyType.ACTION_CTRL,
        "ALT" to KeyType.ACTION_ALT,
        "LEFT" to KeyType.ACTION_LEFT,
        "DOWN" to KeyType.ACTION_DOWN,
        "RIGHT" to KeyType.ACTION_RIGHT,
        "PGDN" to KeyType.ACTION_PGDN
    )

    val (row1, row2) = if (mode == SessionType.TERMINAL) {
        terminalRow1 to terminalRow2
    } else {
        editorRow1 to editorRow2
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(1f)
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
        
        // Editor Symbols
        "{" -> viewModel.onKeyEvent(Key("{", type = KeyType.CHARACTER), KeyGesture.Tap)
        "}" -> viewModel.onKeyEvent(Key("}", type = KeyType.CHARACTER), KeyGesture.Tap)
        "(" -> viewModel.onKeyEvent(Key("(", type = KeyType.CHARACTER), KeyGesture.Tap)
        ")" -> viewModel.onKeyEvent(Key(")", type = KeyType.CHARACTER), KeyGesture.Tap)
        "[" -> viewModel.onKeyEvent(Key("[", type = KeyType.CHARACTER), KeyGesture.Tap)
        "]" -> viewModel.onKeyEvent(Key("]", type = KeyType.CHARACTER), KeyGesture.Tap)
        "<" -> viewModel.onKeyEvent(Key("<", type = KeyType.CHARACTER), KeyGesture.Tap)
        ">" -> viewModel.onKeyEvent(Key(">", type = KeyType.CHARACTER), KeyGesture.Tap)

        // Navigation Keys (now handled by ViewModel)
        "HOME" -> viewModel.onKeyEvent(Key("HOME", type = KeyType.ACTION_HOME), KeyGesture.Tap)
        "END" -> viewModel.onKeyEvent(Key("END", type = KeyType.ACTION_END), KeyGesture.Tap)
        "PGUP" -> viewModel.onKeyEvent(Key("PGUP", type = KeyType.ACTION_PGUP), KeyGesture.Tap)
        "PGDN" -> viewModel.onKeyEvent(Key("PGDN", type = KeyType.ACTION_PGDN), KeyGesture.Tap)
        "UP" -> viewModel.onKeyEvent(Key("UP", type = KeyType.ACTION_UP), KeyGesture.Tap)
        "DOWN" -> viewModel.onKeyEvent(Key("DOWN", type = KeyType.ACTION_DOWN), KeyGesture.Tap)
        "RIGHT" -> viewModel.onKeyEvent(Key("RIGHT", type = KeyType.ACTION_RIGHT), KeyGesture.Tap)
        "LEFT" -> viewModel.onKeyEvent(Key("LEFT", type = KeyType.ACTION_LEFT), KeyGesture.Tap)
    }
}
