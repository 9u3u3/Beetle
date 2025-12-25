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
    val editorFontSize: Int = 14,
    // Left keyboard padding for landscape mode (to avoid camera)
    // Range: 0dp to 50dp
    val leftKeyboardPadding: Float = 0f,
    // Keyboard height settings for landscape mode
    // Range: 0.3 to 0.7 (30% to 70% of screen height)
    val leftKeyboardHeight: Float = 0.5f,
    val rightKeyboardHeight: Float = 0.5f,
    val linkedKeyboardHeight: Boolean = true, // When true, both height sliders move together
    // Landscape IME mode: if true, use system IME keyboard instead of custom split keyboard
    // Default is false (use custom split keyboard)
    val useLandscapeIme: Boolean = false,
    // Bar size settings: scale factor for top bar and bottom bar
    // Range: 0.7 to 1.5 (70% to 150%)
    val topBarScale: Float = 1.0f,
    val bottomBarScale: Float = 1.0f
)