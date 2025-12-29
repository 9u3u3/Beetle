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

# Check Java LSP
if ! command -v jdtls >/dev/null 2>&1; then
    echo "[JAVA] jdtls not found. Installing Java toolchain..." >&2

    if [ "$DISTRO" = "alpine" ]; then
        apk add --no-cache \
            openjdk17-jdk \
            jdtls \
            2>&1 >&2 || true
    else
        apt-get update -qq 2>&1 >&2 || true
        apt-get install -y \
            openjdk-17-jdk \
            jdtls \
            2>&1 >&2 || true
        
        # Fallback: try eclipse-jdt-ls if jdtls not found
        if ! command -v jdtls >/dev/null 2>&1; then
            apt-get install -y eclipse-jdt-ls 2>&1 >&2 || true
        fi
    fi

    if ! command -v jdtls >/dev/null 2>&1; then
        echo "[JAVA] ERROR: jdtls installation failed" >&2
        exit 1
    fi

    echo "[JAVA] Installation completed." >&2
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    echo "[JAVA] Install-only mode, exiting." >&2
    exit 0
fi

# Run the LSP server
echo "[JAVA] jdtls found. Launching LSP..." >&2
exec jdtls