#!/bin/bash
# JavaScript/TypeScript LSP startup script
# IMPORTANT: During LSP mode, stdout must be kept clean for JSON-RPC communication.

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:$HOME/.local/bin
export HOME=/home
export TERM=xterm-256color
export DEBIAN_FRONTEND=noninteractive
export TZ=UTC

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

# Install Node if missing
if ! command -v node >/dev/null 2>&1; then
    log_msg "[JS] Node not found, installing..."
    if [ "$DISTRO" = "alpine" ]; then
        run_silent apk add --no-cache nodejs npm
    else
        run_silent apt-get update -qq || true
        run_silent apt-get install -y nodejs npm || true
    fi
fi

# Install TypeScript + JS language server
if ! command -v typescript-language-server >/dev/null 2>&1; then
    log_msg "[JS] typescript-language-server not found, installing..."
    # A failed earlier run can leave partial folders, and npm then stops with ENOTEMPTY.
    NPM_ROOT=$(npm root -g 2>/dev/null || echo /usr/local/lib/node_modules)
    rm -rf "$NPM_ROOT/typescript" "$NPM_ROOT"/.typescript-* "$NPM_ROOT/typescript-language-server" "$NPM_ROOT"/.typescript-language-server-*
    run_silent npm install -g --cache /tmp/beetle-npm-cache --no-fund --no-audit typescript typescript-language-server
    log_msg "[JS] Installation complete."
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    log_msg "[JS] Install-only mode, exiting."
    exit 0
fi

# Run the LSP server
exec typescript-language-server --stdio
