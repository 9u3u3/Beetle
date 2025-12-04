# Crash Analysis Redux
**Date:** 2025-12-04
**Subject:** Persistent Crash due to `webViewClient` Overwriting

## Executive Summary
The application continues to crash with `SIGTRAP` (renderer process failure) because the fix implemented in `Session.kt` is being **overwritten** by UI components. Specifically, `Editor.kt` and `DevBrowserScreen.kt` assign their own `WebViewClient` instances, which lack the critical `onRenderProcessGone` handler, effectively stripping the protection added in `Session.kt`.

## Root Cause Analysis

### 1. The Mechanism of Failure
Android `WebView` allows only **one** `WebViewClient` at a time. Assigning a new one replaces the old one completely.

1.  **Initialization:** `Session.kt` creates the `CustomWebView` and assigns a "Safe Client" that handles `onRenderProcessGone`.
2.  **UI Composition:** When the user navigates to the Editor or Browser screen, the Composable functions `Editor` and `DevBrowserScreen` run.
3.  **The Overwrite:**
    -   **`Editor.kt` (Line 29):**
        ```kotlin
        webView.webViewClient = object : android.webkit.WebViewClient() { ... }
        ```
    -   **`DevBrowserScreen.kt` (Line 71):**
        ```kotlin
        webView.webViewClient = client
        ```
4.  **The Crash:** These new clients **do not** implement `onRenderProcessGone`. When the renderer crashes, the default behavior (crash app) is triggered.

## Required Fixes

You must implement `onRenderProcessGone` in **every** place where `webViewClient` is assigned.

### 1. Fix `Editor.kt`
Update the `WebViewClient` in `app/src/main/java/com/bsbeetle/bananaspaghetti/ui/cmeditor/Editor.kt`:

```kotlin
webView.webViewClient = object : android.webkit.WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        // ... existing code ...
    }

    // ADD THIS:
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Log.d("EditorUI", "WebView Renderer Crashed")
        if (view == webView) {
            // Optional: notify user or attempt restore
            // view.destroy() // Be careful destroying inside a Composable
        }
        return true // Prevent app crash
    }
}
```

### 2. Fix `DevBrowserScreen.kt`
Update the `client` object in `app/src/main/java/com/bsbeetle/bananaspaghetti/ui/browser/DevBrowserScreen.kt`:

```kotlin
val client = object : android.webkit.WebViewClient() {
    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
        // ... existing code ...
    }

    override fun onPageStarted(...) {
        // ... existing code ...
    }

    // ADD THIS:
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Log.d("DevBrowserUI", "WebView Renderer Crashed")
        return true // Prevent app crash
    }
}
```

## Verification Checklist
- [ ] `Session.kt`: `createEditorView`, `createTerminalView`, `createBrowserView`, `createDemoView` (Already verified)
- [ ] `Editor.kt`: `Editor` composable
- [ ] `DevBrowserScreen.kt`: `DevBrowserScreen` composable

Once these two files are updated, the crash protection will be active even after the UI loads.
