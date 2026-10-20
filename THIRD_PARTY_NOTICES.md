# Third-party notices

Beetle is licensed under the [GNU General Public License v3.0](LICENSE). It includes or depends on the third-party software below. Each part keeps its own license.

## Bundled native binaries (`app/src/main/jniLibs/`)

| File | Project | License |
|---|---|---|
| `libproot.so`, `libproot32.so`, `libproot-xed.so` | [PRoot](https://github.com/termux/proot) (Termux fork) | GPL-2.0-or-later |
| `libtalloc.so` | [talloc](https://talloc.samba.org/) (Samba) | LGPL-3.0-or-later |
| `libaxs.so` | [acodex_server](https://github.com/bajrangCoder/acodex_server) 0.2.7 | See the project repository |

## Bundled web assets (`app/src/main/assets/`)

| Asset | Project | License |
|---|---|---|
| `terminal/xterm.js`, `terminal/xterm-addon-fit.js`, `terminal/xterm.css` | [xterm.js](https://github.com/xtermjs/xterm.js) | MIT |
| `editor/*.bundle.js`, `git/*.bundle.js` | [CodeMirror 6](https://codemirror.net/) and the npm packages in `package.json` | MIT (CodeMirror). See each package for the others. |
| `eruda.js` | [eruda](https://github.com/liriliri/eruda) | MIT |
| `file_icons/` | File-type icons. See the source project of each icon set. | See the source project |

## Fonts (`app/src/main/res/font/`)

| Font | License |
|---|---|
| Iosevka | SIL Open Font License 1.1 |
| Coda | SIL Open Font License 1.1 |
| Born2bSporty FS | See the font author's terms |
| Karmatic Arcade | See the font author's terms |

## Downloaded at run time

The app downloads the [Ubuntu 24.04 base](https://cdimage.ubuntu.com/ubuntu-base/) root file system on first launch. Ubuntu packages use many licenses. See `/usr/share/doc/*/copyright` inside the environment.

## Gradle dependencies

The Android app depends on libraries that Gradle downloads at build time, including AndroidX and Jetpack Compose (Apache-2.0), Kotlin and kotlinx (Apache-2.0), Ktor (Apache-2.0), Eclipse JGit (EDL-1.0), JSch (BSD-style), NanoHTTPD (BSD-3-Clause), and AndroidX Security Crypto with Google Tink (Apache-2.0). See `gradle/libs.versions.toml` and `app/build.gradle.kts` for the full list.
