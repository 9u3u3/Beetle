# Beetle (BITL) - Technical Architecture Reference

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher-playstore.png" alt="Beetle Icon" width="128"/>
  <h3>"Bring Ideas To Life"</h3>
  <p><i>Advanced Android IDE & Pseudo-Terminal Engineering Deep Dive</i></p>
  <p><b>Licensed under the GNU General Public License v3.0</b></p>
</div>

---

Beetle (BITL) is not just a standard wrapper app—it is a heavy engineering project designed to run native Linux binaries, custom proot filesystems, and complex Language Server Protocol (LSP) networks internally on an Android environment that actively restricts standard OS-level execution. This README breaks down the specific architectural pathways used to accomplish this.

## � System Architecture Overview

The UI layer is built exclusively on **Jetpack Compose** following an **MVVM** pattern. Given the complexity of overlapping components (File Trees, Editors, Terminal Sheets), dependencies are deeply injected using `CompositionLocalProvider` (`LocalEditorViewModel`, `LocalTerminalViewModel`, etc.), allowing highly nested composable trees to avoid prop drilling.

---

## 📝 1. The CodeMirror 6 Bridge (`EditorSession`)

Beetle embeds a custom, highly modular CodeMirror 6 environment within a headless Android `WebView` (`CustomWebView`).

**Technical Implementation:**
- **Dynamic Language Loading:** `editor.js` leverages `@codemirror/lang-*` implementations and asynchronous `import()` to load custom plugin bundles.
- **Bi-Directional Bridging:** Kotlin interfaces via `@JavascriptInterface` (`AndroidEditor`, `AndroidInterface`) to handle saves, dirty states, and cursor positions. Memory stats (Java Heap size, Available Memory) are calculated in Kotlin and injected dynamically into JS to prevent OOM errors inside the WebView.
- **Input Mode Hijacking:** Native CodeMirror struggles with Android IME inputs due to Composition events. `editor.js` listens to screen orientations and dynamically swaps `inputmode` between `text` and `none` to force specific software keyboard behaviors, mitigating layout thrashing.
- **DOM Injection:** Floating tooltips, autocomplete dialogs, and LSP error widgets (`.lsp-doc-popup`) are injected directly into the DOM tree, bypassing default CM6 plugin restrictions for maximum touch compatibility.

---

## 💻 2. The Native Pseudo-Terminal (`Tenebrio` & `pty-lib`)

Standard Android APIs lock down access to hardware-level `/dev/pts`. Beetle employs a custom C++ library to simulate a real TTY.

**Technical Implementation:**
- **JNI Binding:** `pty-lib.cpp` binds to `Tenebrio.kt`. It leverages Linux kernel calls: `open("/dev/ptmx")`, `grantpt()`, and `unlockpt()`.
- **Termios TTY Flags:** Through `#include <termios.h>`, `pty-lib` manually constructs the terminal behavior using raw `TCSANOW` manipulations (`tio.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG...)`). This forces raw character processing back to the UI rather than processing through the Dalvik shell.
- **Foreground Service Pipeline:** Android memory managers rapidly kill intensive background apps. The `TerminalService` maintains a constant WakeLock and creates a persistable Notification, keeping the daemonized `proot` shells alive even when Android minimizes the app.

---

## 🐧 3. Native Proot Environment Execution

To bypass the fact that `/system/bin` only contains limited Android shells (mksh), Beetle runs binaries compiled for GNU/Linux using `proot`.

**Technical Implementation:**
- **Filesystem Remapping (`-b` bindings):** `proot` intercepts syscalls, forcing the kernel to translate fake filesystem paths to actual Android storage.
- **Mount Vectors:** 
    - `/proc/self/fd/0` -> `/dev/stdin`
    - `/sdcard` -> Real scoped storage
    - `PREFIX` & `UBUNTU_TMP` -> Isolated application directories `/data/data/.../files/ubuntu`.
- Due to strict linker permissions, we dynamically inject `LD_LIBRARY_PATH` natively inside the C++ fork before `execv()` executes the payload.

---

## 🧠 4. Language Server Protocol (LSP) Engine (`lsp-lib.cpp`)

To bring desktop-class autocompletion, Beetle spawns real Language Servers (like standard `clangd` or NodeJS-based `pyright`).

**Technical Implementation:**
- **The Execution Wrapper:** Setting up an LSP requires raw binary execution outside the shell. `lsp-lib` functions identically to `pty-lib` but bypasses the UI shell. 
- **The `[LSP-BASH]` Subprocess:** Instead of arbitrary binary invocation, `lsp-lib` creates an isolated `/bin/bash` instance executing wrapper scripts (`. ./script_name`).
- **WebSockets & Asynchronous Parsers:** `LiveServerManager` sets up Ktor CIO engines natively exposing ports **8081-8082**. CodeMirror bridges connections locally. `EditorSession` parses stderr output via `WebChromeClient` intercepts `onConsoleMessage`, converting string tokens (`[LSP-STATUS] line:1 col:1`) to JVM state flows synchronously.

---

## 🔒 5. Secure SSH & Isolated Environment Networks

Beetle acts as its own SSH manager, avoiding reliance on external Termux configurations.

**Technical Implementation:**
- **`UbuntuSshKeyManager`**: Generates and parses pure `id_ed25519` key pairs natively via JVM libraries but outputs them precisely inside the mapped `ubuntu/home/.ssh/` tree to fake out GNU SSH commands ran inside `proot`.
- **Isolated JVM Sessions**: Remote SSH logic and local workspace logic are segregated at the `Session` superclass level. A `TerminalSession` can be spawned with a specific `sshConn` argument, which spins up a completely unique PTY process ensuring local host variables do not bleed into the SSH environment.

---

## � 6. Asset Builds and App Compilation

Because the CodeMirror web bundles must be static for file inclusion (`file:///android_asset/`), we bundle NPM dependencies via `Rollup`.

**Compiling the Editor UI:**
```bash
npm install
npm start # Compiles rollups: editor.bundle.js & notebook.bundle.js
```

**Compiling the APK System:**
```bash
# Debug target
./gradlew assembleDebug

# Release explicitly targeted against beetle-release-keystore.jks
./gradlew assembleRelease
```

---

## 🚀 Future Development Roadmap

While the core architecture is stable, the following major features are currently in the pipeline:
- **Agentic AI Integration:** Implementing advanced LLM-driven agent capabilities tailored for direct codebase manipulation and analysis right inside the editor.
- **Cloud-Based Remote Systems:** Extending the SSH architecture to support seamless syncing and execution on managed, scalable cloud instances directly from the mobile UI.