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
    echo "[SQL] Installing Node.js..." >&2
    if [ "$DISTRO" = "alpine" ]; then
        apk add --no-cache nodejs npm 2>&1 >&2 || true
    else
        apt-get update -qq 2>&1 >&2 || true
        apt-get install -y nodejs npm 2>&1 >&2 || true
    fi
fi

if ! command -v node >/dev/null 2>&1; then
    echo "[SQL] ERROR: Node install failed" >&2
    exit 1
fi

# SQL Language Server
if ! command -v sql-language-server >/dev/null 2>&1; then
    echo "[SQL] Installing sql-language-server..." >&2

    npm install -g sql-language-server \
        --no-fund --no-audit \
        2>&1 >&2 || true
fi

if ! command -v sql-language-server >/dev/null 2>&1; then
    echo "[SQL] ERROR: sql-language-server not found" >&2
    exit 1
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    echo "[SQL] Install-only mode, exiting." >&2
    exit 0
fi

# Launch SQL LSP
echo "[SQL] Launching SQL Language Server..." >&2
exec sql-language-server up --method stdio