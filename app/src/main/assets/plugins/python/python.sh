#!/bin/bash
# Python LSP (pylsp) startup script
# IMPORTANT: During LSP mode, stdout must be kept clean for JSON-RPC communication.
# All diagnostic/installation output goes to /dev/null unless in install-only mode.

export PATH=/home/.local/bin:/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin
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

# Check if pylsp and its linters are installed, install if not.
# The base python-lsp-server package ships without pyflakes/pycodestyle, so without them pylsp
# starts fine but never reports any diagnostics.
if ! command -v pylsp > /dev/null 2>&1 || ! python3 -c "import pyflakes, pycodestyle" > /dev/null 2>&1; then
    log_msg "[PYLSP] pylsp or its linters not found, installing..."
    
    # Ensure pip is available
    if ! command -v pip3 > /dev/null 2>&1 && ! command -v pip > /dev/null 2>&1; then
        log_msg "[PYLSP] Installing pip..."
        if [ "$DISTRO" = "alpine" ]; then
            run_silent apk add --no-cache py3-pip python3 || true
        else
            run_silent apt-get update -qq || true
            run_silent apt-get install -y python3-pip python3-venv || true
        fi
    fi
    
    # Install python-lsp-server with the linters that produce its diagnostics
    if command -v pip3 > /dev/null 2>&1; then
        run_silent pip3 install --user --break-system-packages "python-lsp-server[pyflakes,pycodestyle]"
    else
        run_silent pip install --user --break-system-packages "python-lsp-server[pyflakes,pycodestyle]"
    fi
    
    log_msg "[PYLSP] Installation complete."
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    log_msg "[PYLSP] Install-only mode, exiting."
    exit 0
fi

# Run the LSP server
# Note: pylsp communicates via stdin/stdout using JSON-RPC protocol
# The --log-file option redirects verbose logs away from stdout
exec pylsp --log-file /tmp/pylsp.log