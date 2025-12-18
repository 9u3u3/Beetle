package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType
import com.bsbeetle.bananaspaghetti.viewmodel.KeyGesture
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

// App theme colors - matching the existing dark theme
private val BackgroundColor = Color(0xFF1E1E1E) // Matches app's main dark bg
private val KeyColor = Color(0xFF2D2D30) // Slightly lighter for keys
private val KeyPressedColor = Color(0xFF3E3E42) // Pressed state
private val ActiveColor = Color(0xFF007ACC) // Blue accent for active keys
private val TextColor = Color(0xFFCCCCCC) // Light text

@Composable
fun ExtraKeysRow(
        keyboardViewModel: KeyboardViewModel,
        mode: SessionType,
        modifier: Modifier = Modifier
) {
        val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
        
        // Collapsible state - persisted across recompositions
        var isExpanded by rememberSaveable { mutableStateOf(true) }
        val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 0f else 180f,
                label = "chevronRotation"
        )

        // Row 1 Keys: ESC, /, -, HOME, ↑, END, PGUP
        val row1Keys =
                listOf(
                        KeyData("ESC", "ESC", KeyType.ACTION_ESCAPE),
                        KeyData("/", "/", KeyType.CHARACTER),
                        KeyData("−", "-", KeyType.CHARACTER), // Minus sign
                        KeyData("HOME", "HOME", KeyType.ACTION_HOME),
                        KeyData("↑", "UP", KeyType.ACTION_UP),
                        KeyData("END", "END", KeyType.ACTION_END),
                        KeyData("PGUP", "PGUP", KeyType.ACTION_PGUP)
                )

        // Row 2 Keys: ⇥, CTRL, ALT, ←, ↓, →, PGDN
        val row2Keys =
                listOf(
                        KeyData("⇥", "TAB", KeyType.ACTION_TAB),
                        KeyData("CTRL", "CTRL", KeyType.ACTION_CTRL),
                        KeyData("ALT", "ALT", KeyType.ACTION_ALT),
                        KeyData("←", "LEFT", KeyType.ACTION_LEFT),
                        KeyData("↓", "DOWN", KeyType.ACTION_DOWN),
                        KeyData("→", "RIGHT", KeyType.ACTION_RIGHT),
                        KeyData("PGDN", "PGDN", KeyType.ACTION_PGDN)
                )

        Column(
                modifier =
                        modifier.fillMaxWidth()
                                .zIndex(1f)
                                .background(BackgroundColor)
        ) {
                // Chevron toggle bar - always visible
                Box(
                        modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clickable { isExpanded = !isExpanded },
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = TextColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                        .size(20.dp)
                                        .rotate(chevronRotation)
                        )
                }
                
                // Keys content - collapsible
                AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                ) {
                        Column(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                                // Row 1
                                Row(
                                        modifier = Modifier.fillMaxWidth().height(38.dp).padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                        row1Keys.forEach { keyData ->
                                                AssistantKeyButton(
                                                        displayLabel = keyData.displayLabel,
                                                        isActive = false,
                                                        onClick = {
                                                                handleExtraKey(
                                                                        keyData.actionLabel,
                                                                        keyboardViewModel
                                                                )
                                                        }
                                                )
                                        }
                                }

                                // Row 2
                                Row(
                                        modifier = Modifier.fillMaxWidth().height(38.dp).padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                        row2Keys.forEach { keyData ->
                                                val isActive =
                                                        when (keyData.actionLabel) {
                                                                "CTRL" -> uiState.isCtrlOn
                                                                "ALT" -> uiState.isAltOn
                                                                else -> false
                                                        }
                                                AssistantKeyButton(
                                                        displayLabel = keyData.displayLabel,
                                                        isActive = isActive,
                                                        onClick = {
                                                                handleExtraKey(
                                                                        keyData.actionLabel,
                                                                        keyboardViewModel
                                                                )
                                                        }
                                                )
                                        }
                                }
                        }
                }
        }
}

// Data class to hold key information
private data class KeyData(
        val displayLabel: String, // What to show on the key
        val actionLabel: String, // What to pass to handler
        val type: KeyType
)

@Composable
private fun RowScope.AssistantKeyButton(
        displayLabel: String,
        isActive: Boolean,
        onClick: () -> Unit
) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val backgroundColor =
                when {
                        isActive -> ActiveColor
                        isPressed -> KeyPressedColor
                        else -> KeyColor
                }

        Box(
                modifier =
                        Modifier.weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(backgroundColor)
                                .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = onClick
                                ),
                contentAlignment = Alignment.Center
        ) {
                Text(
                        text = displayLabel,
                        color = TextColor,
                        fontSize = if (displayLabel.length <= 2) 16.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                )
        }
}

// Keep the old ExtraKeyButton for backward compatibility if needed
@Composable
fun ExtraKeyButton(label: String, isActive: Boolean, onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val backgroundColor =
                when {
                        isActive -> ActiveColor
                        isPressed -> KeyPressedColor
                        else -> KeyColor
                }

        Box(
                modifier =
                        Modifier.height(34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(backgroundColor)
                                .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = onClick
                                )
                                .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
        ) {
                Text(
                        text = label,
                        color = TextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                )
        }
}

fun handleExtraKey(label: String, viewModel: KeyboardViewModel) {
        when (label) {
                "ESC" ->
                        viewModel.onKeyEvent(
                                Key("ESC", type = KeyType.ACTION_ESCAPE),
                                KeyGesture.Tap
                        )
                "/" -> viewModel.onKeyEvent(Key("/", type = KeyType.CHARACTER), KeyGesture.Tap)
                "-" -> viewModel.onKeyEvent(Key("-", type = KeyType.CHARACTER), KeyGesture.Tap)
                "TAB" -> viewModel.onKeyEvent(Key("TAB", type = KeyType.ACTION_TAB), KeyGesture.Tap)
                "CTRL" -> {
                        // Toggle CTRL state and sync with terminal JavaScript
                        viewModel.onKeyEvent(
                                Key("CTRL", type = KeyType.ACTION_CTRL),
                                KeyGesture.Tap
                        )

                        // For terminal sessions, also tell JS that CTRL is toggled
                        val session =
                                com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
                                        .sessions.getOrNull(
                                        com.bsbeetle
                                                .bananaspaghetti
                                                .utils
                                                .sessionHandling
                                                .SessionList
                                                .activeSessionIndex
                                                ?: -1
                                )
                        if (session is
                                        com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
                        ) {
                                // Get the new CTRL state after toggle
                                val isCtrlOn = viewModel.uiState.value.isCtrlOn

                                // Set up callback to reset CTRL state when JS uses it
                                if (isCtrlOn) {
                                        session.onCtrlUsed = {
                                                // Reset CTRL state in ViewModel (from background
                                                // thread)
                                                android.os.Handler(
                                                                android.os.Looper.getMainLooper()
                                                        )
                                                        .post {
                                                                viewModel.onKeyEvent(
                                                                        Key(
                                                                                "CTRL",
                                                                                type =
                                                                                        KeyType.ACTION_CTRL
                                                                        ),
                                                                        KeyGesture.Tap
                                                                )
                                                        }
                                        }
                                } else {
                                        session.onCtrlUsed = null
                                }

                                // Tell JS about the CTRL state so it can intercept next character
                                session.view?.post {
                                        session.view?.evaluateJavascript(
                                                "window.setCtrlState($isCtrlOn)",
                                                null
                                        )
                                }
                        }
                }
                "ALT" -> viewModel.onKeyEvent(Key("ALT", type = KeyType.ACTION_ALT), KeyGesture.Tap)

                // Navigation Keys
                "HOME" ->
                        viewModel.onKeyEvent(
                                Key("HOME", type = KeyType.ACTION_HOME),
                                KeyGesture.Tap
                        )
                "END" -> viewModel.onKeyEvent(Key("END", type = KeyType.ACTION_END), KeyGesture.Tap)
                "PGUP" ->
                        viewModel.onKeyEvent(
                                Key("PGUP", type = KeyType.ACTION_PGUP),
                                KeyGesture.Tap
                        )
                "PGDN" ->
                        viewModel.onKeyEvent(
                                Key("PGDN", type = KeyType.ACTION_PGDN),
                                KeyGesture.Tap
                        )
                "UP" -> viewModel.onKeyEvent(Key("UP", type = KeyType.ACTION_UP), KeyGesture.Tap)
                "DOWN" ->
                        viewModel.onKeyEvent(
                                Key("DOWN", type = KeyType.ACTION_DOWN),
                                KeyGesture.Tap
                        )
                "RIGHT" ->
                        viewModel.onKeyEvent(
                                Key("RIGHT", type = KeyType.ACTION_RIGHT),
                                KeyGesture.Tap
                        )
                "LEFT" ->
                        viewModel.onKeyEvent(
                                Key("LEFT", type = KeyType.ACTION_LEFT),
                                KeyGesture.Tap
                        )

                // Editor Symbols (kept for backward compatibility)
                "{" -> viewModel.onKeyEvent(Key("{", type = KeyType.CHARACTER), KeyGesture.Tap)
                "}" -> viewModel.onKeyEvent(Key("}", type = KeyType.CHARACTER), KeyGesture.Tap)
                "(" -> viewModel.onKeyEvent(Key("(", type = KeyType.CHARACTER), KeyGesture.Tap)
                ")" -> viewModel.onKeyEvent(Key(")", type = KeyType.CHARACTER), KeyGesture.Tap)
                "[" -> viewModel.onKeyEvent(Key("[", type = KeyType.CHARACTER), KeyGesture.Tap)
                "]" -> viewModel.onKeyEvent(Key("]", type = KeyType.CHARACTER), KeyGesture.Tap)
                "<" -> viewModel.onKeyEvent(Key("<", type = KeyType.CHARACTER), KeyGesture.Tap)
                ">" -> viewModel.onKeyEvent(Key(">", type = KeyType.CHARACTER), KeyGesture.Tap)
        }
}
