package com.bsbeetle.bananaspaghetti.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Keyboard commands
sealed class EditorAction {
    data class InsertText(val text: String) : EditorAction()
    object Backspace : EditorAction()
    object Enter: EditorAction()
}
class KeyboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    private val _editorAction = MutableSharedFlow<EditorAction>()
    val editorAction = _editorAction.asSharedFlow()



    fun onKeyPress(key: Char) {
        viewModelScope.launch {
            // Logic to determine upper/lower case remains the same
            val textToInsert = if (_uiState.value.isCapsLockOn) {
                key.uppercase()
            } else {
                key.toString()
            }
            // Instead of updating local state, EMIT AN EVENT
            _editorAction.emit(EditorAction.InsertText(textToInsert))
        }
    }

    fun onBackspace() {
        viewModelScope.launch {
            // Instead of updating local state, EMIT AN EVENT
            _editorAction.emit(EditorAction.Backspace)
        }
    }

    fun onEnter() {
        viewModelScope.launch {
            // Instead of updating local state, EMIT AN EVENT
            _editorAction.emit(EditorAction.Enter)
        }
    }

    fun onToggleCapsLock() {
        _uiState.update { currentState ->
            currentState.copy(isCapsLockOn = !currentState.isCapsLockOn)
        }
    }

    fun toggleKeyboardVisibility() {
        _uiState.update { currentState ->
            currentState.copy(toggleKeyboard = !currentState.toggleKeyboard)
        }
    }
}
