package com.bsbeetle.bananaspaghetti.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.data.Ic
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Key action definitions
sealed class KeyGesture {
    object Press : KeyGesture()
    object Release : KeyGesture()
    object Tap : KeyGesture()
    object DoubleTap : KeyGesture()
    object LongPress : KeyGesture()
}


// Editor command definitions
sealed class EditorAction {
    data class InsertText(val text: String) : EditorAction()
    object Backspace : EditorAction()
    object Enter : EditorAction()
    // for the ctrl
    object Copy : EditorAction()
    object Paste : EditorAction()
    object Cut : EditorAction()
    object Undo : EditorAction()
    object Redo : EditorAction()
    object SelectAll : EditorAction()
    object Find : EditorAction()
    object Save : EditorAction()
    object CtrlC : EditorAction()
    object CtrlZ : EditorAction()
    object CtrlD : EditorAction()
    object Escape : EditorAction()
}

class KeyboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    private val _editorAction = MutableSharedFlow<EditorAction>()
    val editorAction = _editorAction.asSharedFlow()


    // only Entry point for the key clicked
    fun onKeyEvent(key: Key, gesture: KeyGesture) {
        when (key.type) {
            KeyType.CHARACTER -> handleCharacter(key, gesture)
            KeyType.ACTION_BACKSPACE -> handleBackspace(gesture)
            KeyType.ACTION_SHIFT -> handleShift(gesture)
            KeyType.ACTION_ENTER -> handleEnter(gesture)
            KeyType.ACTION_SPACE -> handleSpace(gesture)
            KeyType.ACTION_TAB -> handleTab(gesture)
            KeyType.ACTION_TOGGLE -> if (gesture == KeyGesture.Tap) toggleKeyboardVisibility()
            KeyType.ACTION_CTRL -> handleCtrl(gesture)
            KeyType.ACTION_ESCAPE -> handleEscape(gesture)
        }
    }

    private fun handleCharacter(key: Key, gesture: KeyGesture) {
        when (gesture) {
            KeyGesture.Tap -> {
                // Check if CTRL is pressed for shortcuts
                if (_uiState.value.isCtrlOn) {
                    handleCtrlShortcut(key)
                } else {
                    // Single tap means single character
                    val textToInsert = getCharacterText(key)
                    emitText(textToInsert)
                }
            }
            KeyGesture.LongPress -> {
                // Long press/hold = continuous typing of the same character
                if (!_uiState.value.isCtrlOn) {
                    val textToInsert = getCharacterText(key)
                    emitText(textToInsert)
                }
            }
            else -> {}
        }
    }

    private fun handleEscape(gesture: KeyGesture) {
        if (gesture == KeyGesture.Tap) {
            viewModelScope.launch {
                _editorAction.emit(EditorAction.Escape)
            }
            _uiState.update { currentState ->
                currentState.copy(isEscapeOn = !currentState.isEscapeOn)
            }
        }
    }


    private fun handleCtrlShortcut(key: Key) {
        val action = when (key.text.lowercase()) {
            "c" -> EditorAction.CtrlC  // Always interrupt
            "v" -> EditorAction.Paste
            "x" -> EditorAction.Cut
            "d" -> EditorAction.CtrlD
            "z" -> EditorAction.Undo
            "y" -> EditorAction.Redo
            "a" -> EditorAction.SelectAll
            "f" -> EditorAction.Find
            "s" -> EditorAction.Save
            else -> return
        }

        action?.let {
            emitAction(it)
            // Turn off CTRL immediately after shortcut
            _uiState.update { state -> state.copy(isCtrlOn = false) }
        }
    }

    private fun handleCtrl(gesture: KeyGesture) {
        if (gesture == KeyGesture.Tap) {
            _uiState.update { currentState ->
                currentState.copy(isCtrlOn = !currentState.isCtrlOn)
            }
        }
    }

    private fun getCharacterText(key: Key): String {
        return if (_uiState.value.isCapsLockOn) {
            // If caps lock is on, use the shifted character
            key.shiftText ?: key.text.uppercase()
        } else if (_uiState.value.isShiftOn) {
            // If shift is on, use the shifted character AND turn shift off
            _uiState.update { it.copy(isShiftOn = false) }
            key.shiftText ?: key.text.uppercase()
        } else {
            // Otherwise, use the normal character
            key.text
        }
    }

    private fun handleBackspace(gesture: KeyGesture) {
        when (gesture) {
            KeyGesture.Tap -> {
                // Single tap = delete one character
                onBackspace()
            }
            KeyGesture.LongPress -> {
                // Long press/hold = continuous deletion
                onBackspace()
            }
            else -> {} // Do nothing on other gestures
        }
    }

    private fun handleShift(gesture: KeyGesture) {
        when (gesture) {
            KeyGesture.Tap -> _uiState.update { currentState ->
                if (currentState.isCapsLockOn){
                    currentState.copy(isCapsLockOn = false)
                } else {
                    currentState.copy(isShiftOn = !currentState.isShiftOn)  // This line is correct
                }
            }

            KeyGesture.DoubleTap -> _uiState.update { currentState ->
                currentState.copy(isCapsLockOn = !currentState.isCapsLockOn, isShiftOn = false)
            }
            KeyGesture.Press -> _uiState.update { currentState ->
                // If caps lock is on, pressing shift toggles it off temporarily
                // Otherwise, it just turns on shift
                if (currentState.isCapsLockOn) currentState else currentState.copy(isShiftOn = true)
            }
            KeyGesture.Release -> _uiState.update { currentState ->
                // If caps lock is on, releasing shift does nothing
                // Otherwise, it turns shift off
                if (currentState.isCapsLockOn) currentState else currentState.copy(isShiftOn = false)
            }
            else -> {}
        }
    }


    private fun handleEnter(gesture: KeyGesture) {
        when (gesture) {
            KeyGesture.Tap -> onEnter()
            KeyGesture.LongPress -> onEnter() // Allow continuous enter if held
            else -> {}
        }
    }

    private fun handleSpace(gesture: KeyGesture) {
        when (gesture) {
            KeyGesture.Tap, KeyGesture.LongPress -> emitText(" ")
            else -> {}
        }
    }

    private fun handleTab(gesture: KeyGesture) {
        when (gesture) {
            KeyGesture.Tap , KeyGesture.LongPress -> {
                val spaces = " ".repeat(uiState.value.indentValue) // prints x spaces
                emitText(spaces)
            }
            else -> {}
        }

    }
    fun toggleKeyboardVisibility() {
        _uiState.update { currentState ->
            currentState.copy(toggleKeyboard = !currentState.toggleKeyboard)
        }
    }

    fun toggleLeftKeyboardVisibility() {
        _uiState.update { currentState ->
            currentState.copy(isLeftKeyboardVisible = !currentState.isLeftKeyboardVisible)
        }

    }
    fun toggleRightKeyboardVisibility() {
        _uiState.update { currentState ->
            currentState.copy(isRightKeyboardVisible = !currentState.isRightKeyboardVisible)
        }
    }

    fun updateIndentSize(newSize: Int) {
        _uiState.update { currentState ->
            currentState.copy(indentValue = newSize)
        }
    }


    // Emitter functions -> Basic it helps to implements better things

    private fun emitText(text: String) {
        viewModelScope.launch {
            // _editorAction.emit(EditorAction.InsertText(text))
            Ic.ic?.commitText(text, 1)
        }
    }

    private fun onBackspace() {
        viewModelScope.launch {
            //_editorAction.emit(EditorAction.Backspace)
            Ic.ic?.deleteSurroundingText(1, 0)
        }
    }

    private fun onEnter() {
        viewModelScope.launch {
            // _editorAction.emit(EditorAction.Enter)
            // First try key event (lets web editors intercept Enter)
            val sent = Ic.ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)) == true &&
                    Ic.ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)) == true

            // If that didn’t trigger (common in WebViews), manually insert newline
            if (!sent) {
                Ic.ic?.commitText("\n", 1)
            }
        }
    }

    private fun emitAction(action: EditorAction) {
        viewModelScope.launch {
            _editorAction.emit(action)
        }
    }
}