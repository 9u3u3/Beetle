package com.bsbeetle.bananaspaghetti.viewmodel


data class KeyboardUiState(
    val isShiftOn: Boolean=false,
    val isCapsLockOn: Boolean = false,
    val toggleKeyboard: Boolean = false, // Added this line, default to true (visible)
    val isLeftKeyboardVisible: Boolean = true,
    val isRightKeyboardVisible: Boolean = true
// Add other state properties here for control and etc
)