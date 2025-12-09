package com.bsbeetle.bananaspaghetti.viewmodel


data class KeyboardUiState(
    val isShiftOn: Boolean=false,
    val isCapsLockOn: Boolean = false,
    val isCtrlOn: Boolean = false,
    val isAltOn: Boolean = false,
    val isEscapeOn: Boolean=false,
    val keyboardPage: Int = 0, // 0=alphabets, 1=symbols, 2=navigation
    val isLeftKeyboardVisible: Boolean = true,
    val isRightKeyboardVisible: Boolean = true,
    val indentValue:Int=4
)