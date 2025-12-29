#!/bin/bash
set -ex  # Exit immediately on failure and print commands

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin:$HOME/.local/bin
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

# Check if pylsp is installed, install if not
if ! command -v pylsp > /dev/null 2>&1; then
    echo "[PYLSP] pylsp not found, installing..." >&2
    
    # Ensure pip is available
    if ! command -v pip3 > /dev/null 2>&1 && ! command -v pip > /dev/null 2>&1; then
        echo "[PYLSP] Installing pip..." >&2
        if [ "$DISTRO" = "alpine" ]; then
            apk add --no-cache py3-pip python3 2>&1 >&2 || true
        else
            apt-get update -qq 2>&1 >&2 || true
            apt-get install -y python3-pip python3-venv 2>&1 >&2 || true
        fi
    fi
    
    # Install python-lsp-server
    if command -v pip3 > /dev/null 2>&1; then
        pip3 install --user --break-system-packages python-lsp-server 2>&1 >&2
    else
        pip install --user --break-system-packages python-lsp-server 2>&1 >&2
    fi
    
    # Update PATH to include user bin
    export PATH="$HOME/.local/bin:$PATH"
    
    echo "[PYLSP] Installation complete." >&2
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    echo "[PYLSP] Install-only mode, exiting." >&2
    exit 0
fi

# Run the LSP server
exec pylsp --log-file /tmp/pylsp.log -v