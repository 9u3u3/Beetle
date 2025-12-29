#!/bin/bash
set -ex

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:$HOME/.local/bin
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

if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

# Install Node if missing
if ! command -v node >/dev/null; then
    if [ "$DISTRO" = "alpine" ]; then
        apk add --no-cache nodejs npm
    else
        apt-get update -qq 2>&1 >&2 || true
        apt-get install -y nodejs npm 2>&1 >&2 || true
    fi
fi

# Install HTML language server
if ! command -v vscode-html-language-server >/dev/null; then
    npm install -g vscode-langservers-extracted
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    echo "[HTML] Install-only mode, exiting." >&2
    exit 0
fi

exec vscode-html-language-server --stdio
