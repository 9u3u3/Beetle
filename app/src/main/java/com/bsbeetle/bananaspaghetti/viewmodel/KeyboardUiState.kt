package com.bsbeetle.bananaspaghetti.viewmodel


data class KeyboardUiState(
    val isShiftOn: Boolean=false,
    val isCapsLockOn: Boolean = false,
    val isCtrlOn: Boolean = false,
    val isEscapeOn: Boolean=false,
    val toggleKeyboard: Boolean = false, // Added this line, default to true (visible)
    val isLeftKeyboardVisible: Boolean = true,
    val isRightKeyboardVisible: Boolean = true,
    val indentValue:Int=4
// Add other state properties here for control and etc
)