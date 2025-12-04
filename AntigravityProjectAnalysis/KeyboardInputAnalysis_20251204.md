# Keyboard Input Analysis
**Date:** 2025-12-04
**Subject:** Custom Keyboard Input Failure in CodeMirror 6 Editor

## Executive Summary
The custom Android keyboard is failing to input text into the CodeMirror 6 editor because of a mismatch in communication protocols. The Android side uses standard `InputConnection` methods (`commitText`), while the CodeMirror editor is configured to disable standard input (`inputmode="none"`) and instead expects input via specific JavaScript function calls (`window.insertText`).

## Root Cause Analysis

### 1. CodeMirror Configuration (`editor.js`)
The editor is explicitly configured to reject standard virtual keyboard input:
```javascript
EditorView.contentAttributes.of({
  inputmode: "none" // <--- Tells browser/WebView to ignore standard IME
}),
```
This is likely done to prevent the system keyboard from popping up, which is correct since you are using a custom keyboard. However, this also means standard IME events (like those sent by `commitText`) might be ignored or handled inconsistently by the WebView.

### 2. Exposed JavaScript Interface (`editor.js`)
The editor exposes specific global functions to handle input programmatically:
```javascript
window.insertText = function(text) { ... };
window.performBackspace = function() { ... };
window.performEnter = function() { ... };
window.performCopy = function() { ... };
// ... etc
```
These functions directly dispatch transactions to the CodeMirror state, which is the correct and robust way to modify the editor content from outside.

### 3. Android Implementation (`KeyboardViewModel.kt`)
The current implementation tries to use the system's `InputConnection`:
```kotlin
private fun emitText(text: String) {
    viewModelScope.launch {
        // Uses standard IME commitText
        SessionList.sessions[...]._inputConnection?.commitText(text, 1)
    }
}
```
Because `inputmode` is "none", the WebView likely ignores these `commitText` calls or fails to route them to the CodeMirror instance.

## Recommended Solution

Refactor the `KeyboardViewModel` (or the `Session` class) to send input by calling the exposed JavaScript functions instead of using `InputConnection`.

### Implementation Steps

1.  **Add Helper Methods in `Session.kt`**:
    Create methods in the `Session` class to execute the JavaScript commands on the `editorView`.

    ```kotlin
    // In Session.kt
    fun insertTextToEditor(text: String) {
        // Escape the text to prevent JS errors (basic example)
        val escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        editorView?.evaluateJavascript("window.insertText(\"$escapedText\")", null)
    }

    fun performEditorAction(action: String) {
        editorView?.evaluateJavascript("window.$action()", null)
    }
    ```

2.  **Update `KeyboardViewModel.kt`**:
    Modify `emitText`, `onBackspace`, `onEnter`, etc., to call these new `Session` methods.

    ```kotlin
    // In KeyboardViewModel.kt

    private fun emitText(text: String) {
        viewModelScope.launch {
            val session = SessionList.sessions[SessionList.activeSessionIndex!!]
            // Use JS injection for Editor
            if (session.type == SessionType.EDITOR) {
                session.insertTextToEditor(text)
            } else {
                // Fallback for Terminal/Browser (keep existing logic or adapt)
                session._inputConnection?.commitText(text, 1)
            }
        }
    }

    private fun onBackspace() {
        viewModelScope.launch {
            val session = SessionList.sessions[SessionList.activeSessionIndex!!]
            if (session.type == SessionType.EDITOR) {
                session.performEditorAction("performBackspace")
            } else {
                session._inputConnection?.deleteSurroundingText(1, 0)
            }
        }
    }
    ```

### Summary of JS Functions to Map
- `insertText(text)` -> `window.insertText(text)`
- `Backspace` -> `window.performBackspace()`
- `Enter` -> `window.performEnter()`
- `Copy` -> `window.performCopy()`
- `Paste` -> `window.performPaste()`
- `Cut` -> `window.performCut()`
- `Undo` -> `window.performUndo()`
- `Redo` -> `window.performRedo()`
- `SelectAll` -> `window.performSelectAll()`
- `Find` -> `window.performFind()`

This approach bypasses the flaky `InputConnection` and directly controls the editor, ensuring 100% reliability with your custom keyboard.
