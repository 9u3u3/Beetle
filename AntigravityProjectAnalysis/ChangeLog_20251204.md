# Change Log - Crash Fixes
**Date:** 2025-12-04
**Objective:** Fix persistent application crash caused by unhandled `onRenderProcessGone` events in WebViews.

## Changes Applied

### 1. `app/src/main/java/com/bsbeetle/bananaspaghetti/ui/cmeditor/Editor.kt`
- **Action:** Added `onRenderProcessGone` override to the `WebViewClient`.
- **Reason:** The `Editor` composable was overwriting the safe `WebViewClient` from `Session.kt` with one that didn't handle renderer crashes.
- **Implementation:** Returns `true` to prevent the app from crashing.

### 2. `app/src/main/java/com/bsbeetle/bananaspaghetti/ui/browser/DevBrowserScreen.kt`
- **Action:** Added `onRenderProcessGone` override to the `WebViewClient`.
- **Reason:** The `DevBrowserScreen` composable was overwriting the safe `WebViewClient` from `Session.kt`.
- **Implementation:** Returns `true` to prevent the app from crashing.

## User Request Changes (Phase 6)

### 1. Terminal Input & Neovim Fix
- **Problem:** `j`/`k` navigation in Neovim was not working because the WebView's `InputConnection` was intercepting keys or sending them incorrectly to the PTY.
- **Solution:** Implemented direct input injection in `TerminalSession.kt` via `insertText(text: String)` which writes directly to the PTY's `outputFd`. Updated `KeyboardViewModel.kt` to use this method for `TerminalSession`.
- **Benefit:** Reliable input for terminal applications like Neovim.

### 2. Portrait Keyboard Support
- **Problem:** The system soft keyboard was not appearing in portrait mode for the terminal.
- **Solution:** Modified `term.js` to change the textarea's `inputmode` from `none` to `text`. This allows the system to detect an input field and show the keyboard when focused. `SystemUi.kt` already handles hiding it in landscape.
- **Benefit:** Users can use the system keyboard in portrait mode.

### 3. State Preservation
- **Problem:** UI state (e.g., Settings toggles, Recents panel) was lost on orientation change.
- **Solution:** Updated `SettingsScreen.kt` and `HomeScreen.kt` to use `rememberSaveable` instead of `remember` for local state variables (`indentValue`, `tapped`, `isRecentsOpen`).
- **Benefit:** Improved UX by preserving context across rotation.

### 4. ESC Key in Terminal
- **Problem:** The ESC key was not working in the terminal because `handleEscape` was bypassing the session check logic.
- **Solution:** Updated `KeyboardViewModel.kt` to call `emitAction(EditorAction.Escape)` inside `handleEscape`, ensuring the `TerminalSession` check is executed and `\u001B` is sent.
- **Benefit:** Functional ESC key in terminal apps (e.g., exiting insert mode in Neovim).

### 5. Landscape Keyboard Fix
- **Problem:** The system keyboard was popping up in landscape mode when tapping the terminal, despite `SystemUi` settings.
- **Solution:** Updated `CustomWebView.kt` to use `WindowManager` and `defaultDisplay.rotation` for a more reliable orientation check, ensuring `EditorInfo.TYPE_NULL` is set in landscape mode to suppress the keyboard.
- **Benefit:** Correct keyboard behavior in landscape mode.

### 6. Terminal Zoom Fix
- **Problem:** The terminal was zooming when clicked/tapped.
- **Solution:** Added `<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />` to `index.html`.
- **Benefit:** Terminal size remains fixed and consistent.

### 7. Dynamic IME Handling
- **Problem:** The default IME was still popping up in landscape mode in some cases.
- **Solution:** Updated `term.js` to listen for orientation changes and dynamically set `inputmode` to `none` in landscape (hiding IME) and `text` in portrait (showing IME).
- **Benefit:** Robust keyboard behavior matching user preference.

### 8. Portrait Extra Keys Row
- **Problem:** Portrait mode lacked special keys (ESC, CTRL, ALT, Arrows) available in the landscape custom keyboard.
- **Solution:** Implemented `ExtraKeysRow` composable and integrated it into `InitialTerminalUI`. It appears above the system keyboard in portrait mode, providing access to essential terminal keys. Added `ALT` key support to `KeyboardViewModel`.
- **Benefit:** Full terminal functionality in portrait mode.
