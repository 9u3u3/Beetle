package com.bsbeetle.bananaspaghetti.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
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
            KeyType.ACTION_TOGGLE -> if (gesture == KeyGesture.Tap) toggleKeyboardVisibility()
        }
    }


    // To Handle character keys
    private fun handleCharacter(key: Key, gesture: KeyGesture) {
        when (gesture) {
            KeyGesture.Tap -> {
                // Single tap means single character
                val textToInsert = getCharacterText(key)
                emitText(textToInsert)
            }
            KeyGesture.LongPress -> {
                // Long press/hold = continuous typing of the same character
                val textToInsert = getCharacterText(key)
                emitText(textToInsert)
            }
            else -> {} // Ignore other gestures for character keys
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
            KeyGesture.Tap -> onKeyPress(' ')
            KeyGesture.LongPress -> onKeyPress(' ') // Allow continuous space if held
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


    // Emitter functions -> Basic it helps to implements better things

    private fun emitText(text: String) {
        viewModelScope.launch {
            _editorAction.emit(EditorAction.InsertText(text))
        }
    }

    private fun onKeyPress(key: Char) {
        emitText(key.toString())
    }

    private fun onBackspace() {
        viewModelScope.launch {
            _editorAction.emit(EditorAction.Backspace)
        }
    }

    private fun onEnter() {
        viewModelScope.launch {
            _editorAction.emit(EditorAction.Enter)
        }
    }
}