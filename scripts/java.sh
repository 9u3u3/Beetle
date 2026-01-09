#!/bin/bash
# Java LSP (jdtls) startup script
# IMPORTANT: During LSP mode, stdout must be kept clean for JSON-RPC communication.

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin
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

# Check Java LSP
if ! command -v jdtls >/dev/null 2>&1; then
    log_msg "[JAVA] jdtls not found. Installing Java toolchain..."

    if [ "$DISTRO" = "alpine" ]; then
        run_silent apk add --no-cache openjdk17-jdk jdtls || true
    else
        run_silent apt-get update -qq || true
        run_silent apt-get install -y openjdk-17-jdk jdtls || true
        
        # Fallback: try eclipse-jdt-ls if jdtls not found
        if ! command -v jdtls >/dev/null 2>&1; then
            run_silent apt-get install -y eclipse-jdt-ls || true
        fi
    fi

    if ! command -v jdtls >/dev/null 2>&1; then
        log_msg "[JAVA] ERROR: jdtls installation failed"
        exit 1
    fi

    log_msg "[JAVA] Installation completed."
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    log_msg "[JAVA] Install-only mode, exiting."
    exit 0
fi

# Run the LSP server
exec jdtls