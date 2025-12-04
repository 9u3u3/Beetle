package com.bsbeetle.bananaspaghetti.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.model.Key
import com.bsbeetle.bananaspaghetti.model.KeyType
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
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
            KeyType.ACTION_ALT -> handleAlt(gesture)
            KeyType.ACTION_LEFT, KeyType.ACTION_RIGHT, KeyType.ACTION_UP, KeyType.ACTION_DOWN,
            KeyType.ACTION_HOME, KeyType.ACTION_END, KeyType.ACTION_PGUP, KeyType.ACTION_PGDN -> handleNavigation(key.type, gesture)
        }
    }

    fun sendTerminalSequence(sequence: String) {
        emitText(sequence)
    }

    private fun handleCharacter(key: Key, gesture: KeyGesture) {
        when (gesture) {
            KeyGesture.Tap -> {
                // Check if CTRL is pressed for shortcuts
                if (_uiState.value.isCtrlOn) {
                    val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
                    if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession) {
                        // Handle generic Ctrl+Key for Terminal (ASCII control codes)
                        val char = key.text.uppercase()
                        if (char.isNotEmpty()) {
                            val code = char[0].code
                            // ASCII Control codes: A=1 (0x01) ... Z=26 (0x1A)
                            // '@' (64) -> 0 (Null)
                            // '[' (91) -> 27 (Esc)
                            // '\' (92) -> 28 (FS)
                            // ']' (93) -> 29 (GS)
                            // '^' (94) -> 30 (RS)
                            // '_' (95) -> 31 (US)
                            // '?' (63) -> 127 (DEL) - often mapped to Ctrl+?
                            
                            val ctrlCode = if (code == '?'.code) {
                                127
                            } else {
                                code - 64
                            }

                            if (ctrlCode in 0..31 || ctrlCode == 127) {
                                emitText(ctrlCode.toChar().toString())
                            }
                        }
                        // Turn off CTRL
                        _uiState.update { state -> state.copy(isCtrlOn = false) }
                    } else {
                        // Editor / Browser shortcuts
                        handleCtrlShortcut(key, session)
                    }
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
            emitAction(EditorAction.Escape)
            _uiState.update { currentState ->
                currentState.copy(isEscapeOn = !currentState.isEscapeOn)
            }
        }
    }


    private fun handleCtrlShortcut(key: Key, session: com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session?) {
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
            else -> null
        }

        if (action != null) {
            emitAction(action)
            // Turn off CTRL immediately after shortcut
            _uiState.update { state -> state.copy(isCtrlOn = false) }
        } else if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
            // Generic Ctrl+Key for Editor
            session.sendCtrlKey(key.text)
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

    private fun handleAlt(gesture: KeyGesture) {
        if (gesture == KeyGesture.Tap) {
            _uiState.update { currentState ->
                currentState.copy(isAltOn = !currentState.isAltOn)
            }
        }
    }

    private fun handleNavigation(type: KeyType, gesture: KeyGesture) {
        if (gesture == KeyGesture.Tap || gesture == KeyGesture.LongPress) {
            val sequence = when (type) {
                KeyType.ACTION_LEFT -> "\u001B[D"
                KeyType.ACTION_RIGHT -> "\u001B[C"
                KeyType.ACTION_UP -> "\u001B[A"
                KeyType.ACTION_DOWN -> "\u001B[B"
                KeyType.ACTION_HOME -> "\u001B[H"
                KeyType.ACTION_END -> "\u001B[F"
                KeyType.ACTION_PGUP -> "\u001B[5~"
                KeyType.ACTION_PGDN -> "\u001B[6~"
                else -> ""
            }
            if (sequence.isNotEmpty()) {
                sendTerminalSequence(sequence)
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
            val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
            val maxModes = if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession) 3 else 2
            val nextMode = (currentState.keyboardMode + 1) % maxModes
            
            currentState.copy(
                toggleKeyboard = nextMode != 0, // For backward compatibility
                keyboardMode = nextMode
            )
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
            val session = SessionList.sessions[SessionList.activeSessionIndex!!]
            if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
                session.insertTextToEditor(text)
            } else if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession) {
                session.insertText(text)
            } else {
                session._inputConnection?.commitText(text, 1)
            }
        }
    }

    private fun onBackspace() {
        viewModelScope.launch {
            val session = SessionList.sessions[SessionList.activeSessionIndex!!]
            if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
                session.performEditorAction("performBackspace")
            } else if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession) {
                // Send backspace char (0x7F)
                session.insertText("\u007F")
            } else {
                session._inputConnection?.deleteSurroundingText(1, 0)
            }
        }
    }

    private fun onEnter() {
        viewModelScope.launch {
            val session = SessionList.sessions[SessionList.activeSessionIndex!!]
            if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
                session.performEditorAction("performEnter")
            } else if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession) {
                session.insertText("\n")
            } else {
                // First try key event (lets web editors intercept Enter)
                val sent = session._inputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)) == true &&
                        session._inputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)) == true

                // If that didn’t trigger (common in WebViews), manually insert newline
                if (!sent) {
                    session._inputConnection?.commitText("\n", 1)
                }
            }
        }
    }

    private fun emitAction(action: EditorAction) {
        viewModelScope.launch {
            val session = SessionList.sessions[SessionList.activeSessionIndex!!]
            if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
                when (action) {
                    is EditorAction.Copy -> session.performEditorAction("performCopy")
                    is EditorAction.Paste -> session.pasteFromClipboard()
                    is EditorAction.Cut -> session.performEditorAction("performCut")
                    is EditorAction.Undo -> session.performEditorAction("performUndo")
                    is EditorAction.Redo -> session.performEditorAction("performRedo")
                    is EditorAction.SelectAll -> session.performEditorAction("performSelectAll")
                    is EditorAction.Find -> session.performEditorAction("performFind")
                    is EditorAction.CtrlC -> session.performEditorAction("performCopy")
                    is EditorAction.Save -> session.performEditorAction("performSave")
                    is EditorAction.CtrlD -> session.performEditorAction("performDuplicate")
                    // Add other actions as needed
                    else -> _editorAction.emit(action)
                }
            } else if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession) {
                when (action) {
                    is EditorAction.Escape -> session.insertText("\u001B")
                    else -> _editorAction.emit(action)
                }
            } else {
                _editorAction.emit(action)
            }
        }
    }
}