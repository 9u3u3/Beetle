package com.bsbeetle.bananaspaghetti.viewmodel


data class KeyboardUiState(
    val isShiftOn: Boolean = false,
    val isCapsLockOn: Boolean = false,
    val isCtrlOn: Boolean = false,
    val isAltOn: Boolean = false,
    val isEscapeOn: Boolean = false,
    val keyboardPage: Int = 0, // 0=alphabets, 1=symbols, 2=navigation
    val isLeftKeyboardVisible: Boolean = true,
    val isRightKeyboardVisible: Boolean = true,
    val indentValue: Int = 4,
    // Keyboard resize settings: 0.0 to 0.4 (0% to 40% of screen width each)
    // Default 0.2 gives 20:60:20 layout (left:editor:right)
    val leftKeyboardWidth: Float = 0.2f,
    val rightKeyboardWidth: Float = 0.2f,
    val linkedKeyboardSliders: Boolean = true, // When true, both sliders move together
    // Font size settings (8-24 range)
    val terminalFontSize: Int = 14,
    val editorFontSize: Int = 14
)