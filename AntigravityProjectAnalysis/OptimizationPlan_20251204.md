# Project Optimization Plan
**Date:** 2025-12-04
**Objective:** Optimize the BananaSpaghetti project for architecture, performance, and code quality.

## 1. Architectural Refactoring

### 1.1 Split `Session.kt` (The God Object)
**Current State:** `Session.kt` manages all types of WebViews (Editor, Terminal, Browser, Demo), input connections, and Tenebrio instances. It violates the Single Responsibility Principle.
**Plan:**
-   Create an abstract base class `Session`.
-   Create subclasses: `EditorSession`, `TerminalSession`, `BrowserSession`.
-   Move specific logic (e.g., Tenebrio for Terminal, DevTools for Browser) into these subclasses.
-   Create a `SessionManager` to handle the list of sessions.

### 1.2 Fix `SessionList`
**Current State:** `SessionLIst.kt` (Typo!) likely holds a static list.
**Plan:**
-   Rename file to `SessionList.kt`.
-   Ensure safe access to the list.

### 1.3 ViewModel Cleanup
**Current State:** `MainActivity` initializes all ViewModels.
**Plan:**
-   Ensure ViewModels are scoped correctly.
-   Use `AndroidViewModel` where `Application` context is needed to avoid memory leaks in factories.

## 2. Performance Optimization

### 2.1 WebView Pooling
**Current State:** WebViews are created on demand and destroyed manually.
**Plan:**
-   Implement a `WebViewPool` to reuse `CustomWebView` instances.
-   Pre-initialize a WebView in the background if possible.

### 2.2 Keyboard Input Optimization
**Current State:** `InputConnection` is used, which is flaky for CodeMirror.
**Plan:**
-   **Re-implement** the JavaScript injection method for `EditorSession`.
-   This bypasses the Android IME overhead and ensures reliable input.

## 3. Code Quality & Cleanup

### 3.1 Standardization
-   Move hardcoded URLs (`http://localhost:8080`, `file:///android_asset/...`) to a `AppConfig` or `Constants` object.
-   Fix typos (e.g., `LanguagRegistry`).

### 3.2 Crash Protection
-   Ensure `onRenderProcessGone` is implemented in the *base* `WebViewClient` used by the `WebViewPool`.

## 4. Execution Order

1.  **Fix Typos & Constants**: Quick wins to clean up the codebase.
2.  **Refactor Session**: The biggest task. Split `Session.kt`.
3.  **Implement WebView Pool**: Integrate into the new Session architecture.
4.  **Fix Keyboard Input**: Apply the JS injection fix to the new `EditorSession`.
5.  **Verify**: Test all features (Editor typing, Terminal commands, Browser navigation).

## Verification Plan

### Automated Tests
-   Since there are few existing tests, we will rely on manual verification for UI/WebView behavior.

### Manual Verification
-   **Crash Test**: Kill the WebView process (via adb or dev tools) and verify app doesn't crash.
-   **Input Test**: Type fast in the Editor (>50 keys) and verify no lag or stoppage.
-   **Session Switching**: Switch between Editor, Terminal, and Browser to ensure state is preserved.
