# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Beetle (BITL)** — "Bring Ideas To Life" — is a feature-rich Android IDE/terminal app built with Kotlin and Jetpack Compose. It embeds a CodeMirror 6 editor and terminal via WebViews, supports LSP (Language Server Protocol), Git operations (via JGit), and runs a foreground terminal service similar to Termux.

## Build Commands

```bash
# Android build (requires Android SDK 36, CMake 3.22.1, JDK 11+)
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK (signed with beetle-release-keystore.jks — DO NOT sign again)

# Tests
./gradlew test                 # Unit tests (JUnit 4)
./gradlew connectedAndroidTest # Instrumentation tests (requires device/emulator)

# Web asset bundles (CodeMirror editor & notebook)
npm install                    # Install Node dependencies
npm start                      # Rollup bundles → assets/editor/editor.bundle.js & notebook.bundle.js
```

After modifying `assets/editor/editor.js` or `assets/editor/notebook.js`, run `npm start` to regenerate bundles before building the APK.

## Architecture

**MVVM + Compose** with CompositionLocals for dependency injection across the UI tree.

### Source layout (`app/src/main/java/com/bsbeetle/bananaspaghetti/`)

| Package | Purpose |
|---|---|
| `viewmodel/` | ViewModels: `FileViewModel`, `EditorViewModel`, `TerminalViewModel`, `AuthViewModel`, `BufferViewModel`, `KeyboardViewModel`, `LspViewModel`, etc. |
| `ui/navigation/` | Sealed `Screen` class routes + `Navigation.kt` (Compose NavHost) |
| `ui/screens/` | Full-screen composables (Home, Editor, Terminal, Notebook, Browser, Settings, Extensions) |
| `ui/components/` | Shared composables (TopBar, BottomBar, SearchReplaceBar, ExtraKeysRow, dialogs) |
| `ui/data/sidebar/` | Sidebar tabs: FilesTab, SearchTab, SettingsTab |
| `ui/theme/` | BeetleTheme (Material3), SystemUi immersive mode |
| `ui/keyboard/` | Custom keyboard / demo editor UI |
| `utils/webViewHandling/` | `CustomWebView`, `WebViewPool`, content loading helpers |
| `utils/fileHandling/` | `FileOperations`, `FilTree`, `RecentFilesManager`, `UbuntuDocumentProvider` |
| `utils/sessionHandling/` | `SessionList`, WebView-to-session mapping |
| `utils/languageHandling/` | `LanguageRegistry`, `LspServer` (ports 8081-8082) |
| `utils/assetHandling/` | `AssetServer` (localhost:8080 for HTML/JS assets) |
| `directGit/` | JGit wrapper: `GitRepository`, `GitViewModel`, SSH key management, terminal-based Git |
| `liveserver/` | `LiveServerManager`, `LiveHTTPServer` (WebSocket + HTTP preview server) |
| `service/` | `TerminalService` — foreground service with wake lock for background terminal sessions |

### Native code (`app/src/main/cpp/`)

Two C++17 shared libraries built via CMake:
- **pty-lib** — pseudo-terminal for terminal sessions
- **lsp-lib** — native LSP process management

### Web assets (`app/src/main/assets/`)

- `editor/` — CodeMirror 6 editor + notebook bundles (source JS → Rollup → IIFE bundles)
- `terminal/` — Terminal WebView UI
- `scripts/` — Installation/setup scripts
- `git/` — Git UI assets

Rollup config (`rollup.config.js`) produces two bundles with Node polyfills and minification.

### Key ports (local HTTP servers)

| Port | Service |
|---|---|
| 8080 | AssetServer (serves editor/terminal HTML) |
| 8081-8082 | LSP servers |

### Backend

Auth waitlist check via Ktor client → `https://beetle-backend.vercel.app/api/auth/check-waitlist`

## Key Technical Details

- **Compose BOM:** `2024.09.00` — controls all Compose dependency versions
- **Ktor:** v3.3.1 for both server (WebSocket, Netty) and client (CIO)
- **Target architectures:** `arm64-v8a`, `armeabi-v7a`
- **Min SDK:** 26, **Target/Compile SDK:** 36
- **CompositionLocals** (`LocalFileViewModel`, `LocalEditorViewModel`, `LocalTerminalViewModel`, etc.) provide ViewModels throughout the composable tree
- WebViews are pooled (`WebViewPool`) and managed per-session in ViewModels via LRU eviction
