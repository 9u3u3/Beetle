# BananaSpaghetti LSP Implementation Guide

This document provides a comprehensive, end-to-end explanation of how the Language Server Protocol (LSP) system is implemented in BananaSpaghetti. It covers the architecture, native environment, communication bridge, frontend integration, and critical performance optimizations.

## 1. Architecture Overview

The LSP system is a complex integration of Android, Kotlin, C++, and JavaScript. It connects a web-based editor (CodeMirror) running in a WebView to a native Linux binary (`clangd`) running in a proot environment on Android.

**Data Flow:**
`CodeMirror (JS)` <-> `WebSocket` <-> `Ktor Server (Kotlin)` <-> `JNI Bridge` <-> `STDIN/STDOUT` <-> `clangd (Native Linux)`

---

## 2. Native Environment (The "Backend")

Since Android doesn't natively support standard Linux binaries like `clangd` (which rely on glibc), we use **PRoot** to create a virtual Linux environment.

### Components:
*   **Alpine Linux Rootfs:** A minimal Linux filesystem located in `filesDir/alpine`.
*   **PRoot:** A user-space implementation of `chroot`, `mount --bind`, and `binfmt_misc`. It allows us to run Linux binaries on Android without root.
*   **Clangd:** The C++ Language Server binary, running inside the Alpine environment.

### Initialization (`cLsp.kt` & `init-lsp.sh`):
1.  **Asset Extraction:** `cLsp.kt` extracts the Alpine rootfs and `init-lsp.sh` script to the app's internal storage.
2.  **Symlink Creation:** It creates necessary symlinks (e.g., `libtalloc.so`) required by PRoot.
3.  **Process Launch:** The `getCppLsp` JNI function is called. This function:
    *   Sets up the PRoot command arguments.
    *   Binds directories (e.g., `/dev/shm`, `/public`).
    *   Executes `clangd` via PRoot.
    *   **Crucially**, it returns a **Master File Descriptor (FD)** that allows us to read/write to `clangd`'s STDIN and STDOUT directly from Kotlin.

---

## 3. The Communication Bridge (`cLsp.kt`)

We need a way to talk to the running `clangd` process from our Kotlin code and relay that to the JavaScript editor.

### Ktor WebSocket Server:
*   We run an embedded **Netty** server on port `8081`.
*   It exposes a WebSocket endpoint `/clsp`.
*   **Why WebSockets?** They provide a persistent, bi-directional channel perfect for LSP's JSON-RPC messages.

### The Relay Loop:
1.  **Reading from Clangd:**
    *   A coroutine continuously reads from the `input` stream (connected to `clangd`'s STDOUT via the Master FD).
    *   It parses the `Content-Length` header to extract complete JSON-RPC messages.
    *   It sends these JSON messages to the WebSocket client (the editor).
2.  **Writing to Clangd:**
    *   When the WebSocket receives a message from the editor (e.g., `textDocument/didChange`), it parses it.
    *   **URI Injection:** It injects a fake URI (`file:///main.cpp`) because the editor uses a virtual path, but `clangd` needs a consistent file path.
    *   It constructs the LSP header (`Content-Length: ...`) and writes the byte array to the `output` stream (connected to `clangd`'s STDIN).

---

## 4. Frontend Integration (`editor.js`)

The editor is built with **CodeMirror 6**.

### Setup:
*   Connects to `ws://localhost:8081/clsp`.
*   Initializes the LSP session by sending the `initialize` request with capabilities.

### Key Features:
*   **Diagnostics:** Listens for `textDocument/publishDiagnostics` notifications. It maps the line/column ranges to CodeMirror's linting system (`setDiagnosticsEffect`) to underline errors and warnings.
*   **Autocompletion:**
    *   Uses `lspCompletionSource` to fetch suggestions.
    *   Sends `textDocument/completion` requests.
    *   **Custom UI:** Since we use `inputmode="none"` (see below), we render a custom HTML list (`.lsp-completion-list`) for suggestions instead of relying on the browser's native dropdown.

---

## 5. Performance Optimizations (CRITICAL)

This is where the magic happens. Without these, the editor would freeze constantly.

### A. Input Handling (`inputmode="none"`)
*   **Problem:** The default Android soft keyboard + WebView combination is slow and buggy for code editing.
*   **Solution:** We set `inputmode="none"` on the editor in Landscape mode. This disables the system keyboard.
*   **Custom Input:** We built a custom on-screen keyboard (`KeyboardViewModel.kt`).
*   **Buffering:** The keyboard buffers keystrokes into 16ms chunks before sending them to the WebView via `insertText`. This prevents flooding the JS engine with thousands of calls per second.

### B. Debouncing & Throttling (`editor.js`)
*   **`didChange`:** We only send document updates to `clangd` **1 second** after you stop typing. Sending them on every keystroke would choke the server.
*   **Autocompletion:**
    *   Disabled automatic triggering while typing fast.
    *   **Smart Trigger:** It waits for a 300ms pause. Then, it **flushes** any pending `didChange` update immediately before asking for completions. This ensures `clangd` knows about the latest code (e.g., `std::`) without needing a constant stream of updates.

### C. Memory Management
*   **Request Cleanup:** We limit the `pendingRequests` map to 20 items. If requests (like old completion calls) pile up, we delete the oldest ones to prevent memory leaks.
*   **Large File Protection:** We skip sending `didChange` updates if the file is larger than 50KB to prevent freezing the serialization thread.

### D. UI Thread Offloading
*   **Focus Removal:** We removed `view.focus()` calls from `insertText` and other actions. Forcing focus on every keystroke causes expensive layout recalculations (reflows).
*   **Status Updates:** LSP status (line/col/errors) is throttled to update max once every 500ms, and we use `console.log` interception (`WebChromeClient`) instead of `JavascriptInterface` to avoid WebView reloads.

---

## 6. How to Re-implement Yourself

1.  **Get the Binaries:** You need `clangd`, `proot`, and a rootfs.
2.  **Build the JNI:** Write C++ JNI code to fork/exec `proot` and pipe the file descriptors.
3.  **Setup Ktor:** Create a simple WebSocket server that reads from the JNI pipe and writes to it.
4.  **Frontend:**
    *   Use CodeMirror 6.
    *   Implement a `WebSocket` client.
    *   Handle the LSP JSON-RPC protocol (Header + JSON).
    *   **Crucial:** Implement the debouncing logic described above. Do NOT send every keystroke to the server.
5.  **Android Input:**
    *   If building a custom keyboard, batch your inputs.
    *   Use `evaluateJavascript` carefully.

---

## 7. Troubleshooting Common Issues

*   **Freezing on Typing:**
    *   **Cause:** Too many `evaluateJavascript` calls or `didChange` notifications.
    *   **Fix:** Increase debounce times, batch inputs, remove `view.focus()`.
*   **No Suggestions:**
    *   **Cause:** `inputmode="none"` blocks default trigger.
    *   **Fix:** Manually call `startCompletion(view)` in your JS after a delay.
*   **WebView Crashes:**
    *   **Cause:** Memory leaks in JS or native process dying.
    *   **Fix:** Monitor `pendingRequests`, check `clangd` memory usage.
