package com.bsbeetle.bananaspaghetti.viewmodel


data class KeyboardUiState(
    val isShiftOn: Boolean=false,
    val isCapsLockOn: Boolean = false,
    val isCtrlOn: Boolean = false,
    val isAltOn: Boolean = false,
    val isEscapeOn: Boolean=false,
<<<<<<< HEAD
    val toggleKeyboard: Boolean = false,
    val keyboardMode: Int = 0, // 0=QWERTY, 1=Layout2, 2=Layout3
=======
    val keyboardPage: Int = 0, // 0=alphabets, 1=symbols, 2=navigation
>>>>>>> bce1bd3c88405ca2ba91e3f0dba45b8bbaf33cf5
    val isLeftKeyboardVisible: Boolean = true,
    val isRightKeyboardVisible: Boolean = true,
    val indentValue:Int=4
)