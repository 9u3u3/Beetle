#!/bin/bash
set -ex

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin
export HOME=/home
export TERM=xterm-256color

# Check for install-only mode
INSTALL_ONLY=false
if [ "$1" = "--install-only" ]; then
    INSTALL_ONLY=true
fi

# Detect Linux distribution (Alpine vs Ubuntu)
detect_distro() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        echo "$ID"
    elif [ -f /etc/alpine-release ]; then
        echo "alpine"
    elif command -v apt-get >/dev/null 2>&1; then
        echo "ubuntu"
    else
        echo "unknown"
    fi
}

DISTRO=$(detect_distro)

# Ensure linker config exists
if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

# Node.js
if ! command -v node >/dev/null 2>&1; then
    echo "[REACT] Installing Node.js..." >&2
    if [ "$DISTRO" = "alpine" ]; then
        apk add --no-cache nodejs npm 2>&1 >&2 || true
    else
        apt-get update -qq 2>&1 >&2 || true
        apt-get install -y nodejs npm 2>&1 >&2 || true
    fi
fi

if ! command -v node >/dev/null 2>&1; then
    echo "[REACT] ERROR: Node install failed" >&2
    exit 1
fi

# TypeScript + LSP
if ! command -v typescript-language-server >/dev/null 2>&1; then
    echo "[REACT] Installing TypeScript LSP..." >&2

    npm install -g \
        typescript \
        typescript-language-server \
        --no-fund --no-audit \
        2>&1 >&2 || true
fi

if ! command -v typescript-language-server >/dev/null 2>&1; then
    echo "[REACT] ERROR: typescript-language-server not found" >&2
    exit 1
fi

# Optional: ESLint / HTML / CSS
if ! command -v vscode-eslint-language-server >/dev/null 2>&1; then
    echo "[REACT] Installing HTML/CSS/ESLint servers..." >&2
    npm install -g vscode-langservers-extracted \
        --no-fund --no-audit \
        2>&1 >&2 || true
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    echo "[REACT] Install-only mode, exiting." >&2
    exit 0
fi

# Launch React LSP
echo "[REACT] Launching TypeScript Language Server..." >&2
exec typescript-language-server --stdio