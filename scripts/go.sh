#!/bin/bash
# Go LSP (gopls) startup script
# IMPORTANT: During LSP mode, stdout must be kept clean for JSON-RPC communication.

export PATH=/home/go/bin:/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin
export HOME=/home
export TERM=xterm-256color
export DEBIAN_FRONTEND=noninteractive
export TZ=UTC
export GOPATH="$HOME/go"

# Check for install-only mode
INSTALL_ONLY=false
if [ "$1" = "--install-only" ]; then
    INSTALL_ONLY=true
    set -ex  # Enable tracing only during install
fi

# Helper to conditionally output (only during install mode)
log_msg() {
    if [ "$INSTALL_ONLY" = true ]; then
        echo "$1" >&2
    fi
}

# Helper to redirect command output (silent in LSP mode, visible in install mode)
run_silent() {
    if [ "$INSTALL_ONLY" = true ]; then
        "$@" 2>&1
    else
        "$@" >/dev/null 2>&1
    fi
}

# Detect Linux distribution
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

# Create linkerconfig if needed
if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig 2>/dev/null || true
    touch /linkerconfig/ld.config.txt 2>/dev/null || true
fi

# Check Go Toolchain
if ! command -v go >/dev/null 2>&1; then
    log_msg "[GO] Go compiler not found. Installing..."

    if [ "$DISTRO" = "alpine" ]; then
        run_silent apk add --no-cache go || true
    else
        run_silent apt-get update -qq || true
        run_silent apt-get install -y golang-go || true
    fi

    if ! command -v go >/dev/null 2>&1; then
        log_msg "[GO] ERROR: Go installation failed"
        exit 1
    fi

    log_msg "[GO] Go installed successfully."
fi

# Check gopls (Go LSP)
if ! command -v gopls >/dev/null 2>&1; then
    log_msg "[GO] gopls not found. Installing..."

    if [ "$DISTRO" = "alpine" ]; then
        run_silent apk add --no-cache gopls || true
    else
        run_silent apt-get update -qq || true
        run_silent apt-get install -y gopls || true
    fi

    # Fallback: install via go install
    if ! command -v gopls >/dev/null 2>&1; then
        log_msg "[GO] Package gopls failed, installing via go toolchain..."
        export PATH="$GOPATH/bin:$PATH"
        run_silent go install golang.org/x/tools/gopls@latest || true
    fi

    if ! command -v gopls >/dev/null 2>&1; then
        log_msg "[GO] ERROR: gopls installation failed"
        exit 1
    fi

    log_msg "[GO] gopls installed successfully."
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    log_msg "[GO] Install-only mode, exiting."
    exit 0
fi

# Run the LSP server
exec gopls