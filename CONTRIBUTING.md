# Contributing to Beetle

Thank you for your help. This file tells you how to send a change.

## Branches

| Branch | Use |
|---|---|
| `main` | Stable code. Each merge to `main` must build. |
| `develop` | Daily work. Open pull requests against `develop`. |

Make a new branch from `develop` for each change. Use a name that tells the type of change:

- `feature/<short-name>` for a new feature, for example `feature/voice-input`
- `fix/<short-name>` for a bug fix, for example `fix/editor-save-escapes`
- `docs/<short-name>` for documentation only

## Set up the project

Follow the steps in [README.md](README.md#build-from-source). Test on a real `arm64-v8a` or `armeabi-v7a` device. The app does not run on `x86_64` emulators.

## Before you open a pull request

1. Build the web bundles if you changed `editor.js` or `notebook.js`:
   ```bash
   npm start
   ```
2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run the unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Test the change on a device. Tell us in the pull request which device and Android version you used.
5. Add a unit test for logic that can run on the JVM (for example parsers and diff code).

## Code style

- Write Kotlin that looks like the code around it. Use the same names, comment density, and idioms.
- Do not commit secrets: API keys, signing keys, `keystore.properties`, or `local.properties`.
- Do not commit build output, `node_modules/`, or log files.
- Keep each pull request to one topic.

## Commit messages

- Write the first line in the imperative mood, at 72 characters or fewer. For example: `Fix editor save for files that contain backslashes`.
- Add a blank line and a body when the reason for the change is not clear from the code.
- Use respectful language in commit messages, comments, and logs. The [Code of Conduct](CODE_OF_CONDUCT.md) applies to them too.

## Report a bug or ask for a feature

Use the issue templates on GitHub. For a bug, give the device, the Android version, the app version, and the steps that cause the problem.

For a security problem, follow [SECURITY.md](SECURITY.md) and do not open a public issue.

## License of contributions

Beetle uses the [GNU General Public License v3.0](LICENSE). When you send a pull request, you agree that your contribution uses the same license.
