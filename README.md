<div align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="Beetle icon" width="128"/>
  <h1>Beetle (BITL)</h1>
  <p><i>Bring Ideas To Life</i></p>
  <p>A code editor and Linux terminal for Android phones.</p>
  <p>
    <a href="LICENSE"><img alt="License: AGPL v3" src="https://img.shields.io/badge/license-AGPLv3-blue.svg"></a>
    <a href="https://github.com/9u3u3/bananaSpaghetti/actions/workflows/build.yml"><img alt="Build" src="https://github.com/9u3u3/bananaSpaghetti/actions/workflows/build.yml/badge.svg"></a>
  </p>
</div>

---

> **Status: alpha.** Beetle works on the devices we test, but expect bugs. Please
> [report them](https://github.com/9u3u3/bananaSpaghetti/issues).

## Features

- **Code editor.** CodeMirror 6 in a WebView, with syntax highlighting, search and replace, undo and redo, and a split keyboard for landscape mode.
- **Linux terminal.** An Ubuntu 24.04 base system runs through `proot`, with a real PTY and `bash`. A foreground service keeps sessions alive when the app is in the background.
- **Language servers (LSP).** Completion and diagnostics from servers that run inside the Ubuntu environment.
- **Git.** Clone, commit, branch, merge, and push through JGit, with SSH keys stored in the Android Keystore.
- **SSH.** Connect to remote machines, browse their files, and open remote terminals.
- **Python notebook.** Cells that run in the local Python environment.
- **Live preview.** A local HTTP and WebSocket server, plus an in-app browser.
- **AI assistant (optional).** Chat about the open file, or ask for an edit. You bring your own Anthropic or Google Gemini API key (see [AI assistant](#ai-assistant)).

## Requirements

| Item | Version |
|---|---|
| Android device | Android 8.0 (API 26) or later, `arm64-v8a` or `armeabi-v7a` |
| JDK | 17 (the code targets Java 11) |
| Android SDK | API 36, CMake 3.22.1, NDK |
| Node.js | 20 or later, for the editor bundles |

The app has no `x86_64` build, so it does not run on most emulators. Use a real device.

## Build from source

1. Clone the repository:
   ```bash
   git clone https://github.com/9u3u3/bananaSpaghetti.git
   cd bananaSpaghetti
   ```
2. Build the editor bundles:
   ```bash
   npm ci
   npm start
   ```
   Run `npm start` again each time you change `app/src/main/assets/editor/editor.js` or `notebook.js`.
3. Build and install the debug APK on a connected device:
   ```bash
   ./gradlew installDebug
   ```
4. Run the unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

### Release builds

`./gradlew assembleRelease` builds an unsigned APK. To sign it, put a `keystore.properties` file in the repository root, or set the `BEETLE_KEYSTORE_FILE`, `BEETLE_KEYSTORE_PASSWORD`, `BEETLE_KEY_ALIAS`, and `BEETLE_KEY_PASSWORD` environment variables. Git ignores `keystore.properties`, `*.jks`, and `*.keystore`. Never commit signing keys.

On first launch, the app downloads the Ubuntu base system (about 30 MB).

## Architecture

The UI uses Jetpack Compose with the MVVM pattern. `CompositionLocal` values give ViewModels to the composable tree (`LocalEditorViewModel`, `LocalTerminalViewModel`, and others).

| Part | Where | What it does |
|---|---|---|
| Editor | `assets/editor/`, `utils/sessionHandling/EditorSession.kt` | CodeMirror 6, bundled with Rollup. Kotlin and JavaScript talk through `@JavascriptInterface` bridges. |
| Terminal | `tenebrio/pty/`, `cpp/pty-lib.cpp` | A C++ JNI library opens `/dev/ptmx` and starts `proot` with the Ubuntu root file system. |
| LSP | `cpp/lsp-lib.cpp`, `utils/languageHandling/` | Starts language servers inside Ubuntu and bridges them to the editor on ports 8081–8082. |
| Git | `directGit/` | JGit, with SSH through JSch. JSch cannot make ED25519 keys, so the app makes RSA-4096 keys. |
| Live server | `liveserver/` | Ktor HTTP and WebSocket preview server. |
| Assets | `utils/assetHandling/AssetServer.kt` | Serves the editor and terminal pages on `localhost:8080`. |
| AI | `utils/ai/`, `viewmodel/AiChatViewModel.kt` | Provider interface with Anthropic and Gemini implementations. |

All Kotlin source is in `app/src/main/java/com/bsbeetle/bananaspaghetti/`.

## AI assistant

The AI assistant is off until you add an API key in **Settings → AI**. Beetle has no AI key of its own. Your requests go from your phone straight to the provider you choose, and the provider bills you directly.

- **Anthropic (Claude):** get a key at <https://console.anthropic.com/settings/keys>.
- **Google (Gemini):** get a key at <https://aistudio.google.com/apikey>. Gemini has a free tier.

The app stores keys with `EncryptedSharedPreferences` (Android Keystore). Android backups do not include the keys.

## Privacy and network use

Beetle sends data to these services:

| Service | When | What it sends |
|---|---|---|
| `beetle-backend.vercel.app` | Update check, extension list | The request only |
| `beetle-backend.vercel.app` | You submit **Report issue** | Your email, your description, and any image you attach |
| `beetle-backend.vercel.app` | After a crash, on the next launch | Stack trace, exception message, app version, device model, Android version, and the email you entered. It does not send file contents, project source, or API keys. Turn it off in **Settings → AI**. |
| Anthropic or Google | Only when you use the AI assistant | Your messages and the content of the open file |
| Ubuntu mirrors | First launch, and when you run `apt` | Package downloads |

If you fork Beetle, change the backend URLs in `utils/UpdatesService.kt`, `utils/diagnostics/CrashReporter.kt`, `ui/Extension/ExtensionsViewModel.kt`, and `ui/home/ReportIssueDialog.kt`.

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before you open a pull request. Everyone in this project follows the [Code of Conduct](CODE_OF_CONDUCT.md).

To report a security problem, follow [SECURITY.md](SECURITY.md). Do not open a public issue for it.

## License

Copyright (C) 2025-2026 The Beetle authors.

Beetle is free software under the [GNU Affero General Public License v3.0 or later](LICENSE) (`AGPL-3.0-or-later`).

In short, if you share Beetle or a changed version of it, you must:

- give the full source code, under the same license, to everyone who gets the app;
- keep the copyright and license notices;
- mark the changes that you made;
- give the source code to users who use your changed version over a network, for example through a hosted service.

You can use and change Beetle privately without publishing your changes. The license text in [LICENSE](LICENSE) is the legal reference. This summary is not legal advice.

Beetle includes third-party software under other licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
