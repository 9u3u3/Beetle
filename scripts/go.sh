#!/bin/bash
set -ex  # Exit immediately on failure and print commands

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

# Check Go Toolchain
if ! command -v go >/dev/null 2>&1; then
    echo "[GO] Go compiler not found. Installing..." >&2

    if [ "$DISTRO" = "alpine" ]; then
        apk add --no-cache go 2>&1 >&2 || true
    else
        apt-get update -qq 2>&1 >&2 || true
        apt-get install -y golang-go 2>&1 >&2 || true
    fi

    if ! command -v go >/dev/null 2>&1; then
        echo "[GO] ERROR: Go installation failed" >&2
        exit 1
    fi

    echo "[GO] Go installed successfully." >&2
fi

# Check gopls (Go LSP)
if ! command -v gopls >/dev/null 2>&1; then
    echo "[GO] gopls not found. Installing..." >&2

    if [ "$DISTRO" = "alpine" ]; then
        apk add --no-cache gopls 2>&1 >&2 || true
    else
        apt-get update -qq 2>&1 >&2 || true
        apt-get install -y gopls 2>&1 >&2 || true
    fi

    # Fallback: install via go install
    if ! command -v gopls >/dev/null 2>&1; then
        echo "[GO] Package gopls failed, installing via go toolchain..." >&2

        export GOPATH="$HOME/go"
        export PATH="$GOPATH/bin:$PATH"

        go install golang.org/x/tools/gopls@latest 2>&1 >&2 || true
    fi

    if ! command -v gopls >/dev/null 2>&1; then
        echo "[GO] ERROR: gopls installation failed" >&2
        exit 1
    fi

    echo "[GO] gopls installed successfully." >&2
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    echo "[GO] Install-only mode, exiting." >&2
    exit 0
fi

# Run the LSP server
echo "[GO] gopls found. Launching LSP..." >&2
exec gopls