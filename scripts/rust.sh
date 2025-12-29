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

# Check rust-analyzer
if ! command -v rust-analyzer >/dev/null 2>&1; then
    echo "[RUST] rust-analyzer not found. Installing toolchain..." >&2

    # Install Rust toolchain
    if ! command -v rustc >/dev/null 2>&1; then
        if [ "$DISTRO" = "alpine" ]; then
            apk add --no-cache \
                rust \
                rustfmt \
                rust-clippy \
                musl-dev \
                gcc \
                libc-dev \
                pkgconfig \
                2>&1 >&2 || true
        else
            apt-get update -qq 2>&1 >&2 || true
            apt-get install -y \
                rustc \
                cargo \
                rustfmt \
                build-essential \
                2>&1 >&2 || true
        fi
    fi

    # Install rust-analyzer
    if [ "$DISTRO" = "alpine" ]; then
        apk add --no-cache rust-analyzer 2>&1 >&2 || true
    else
        apt-get install -y rust-analyzer 2>&1 >&2 || true
    fi

    if ! command -v rust-analyzer >/dev/null 2>&1; then
        echo "[RUST] ERROR: rust-analyzer installation failed" >&2
        exit 1
    fi

    echo "[RUST] Installation completed." >&2
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    echo "[RUST] Install-only mode, exiting." >&2
    exit 0
fi

# Run the LSP server
echo "[RUST] rust-analyzer found. Launching LSP..." >&2
exec rust-analyzer