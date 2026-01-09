#!/bin/bash
# C/C++ LSP (clangd) startup script
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

# Determine initrc path based on distro
if [ "$DISTRO" = "alpine" ]; then
    INITRC_PATH="$PREFIX/alpine/initrc"
else
    INITRC_PATH="$PREFIX/ubuntu/initrc"
fi

# Create initrc if it doesn't exist
if [ ! -e "$INITRC_PATH" ]; then
    cat <<'EOF' > "$INITRC_PATH"
if [ -f ~/.bashrc ]; then
    . ~/.bashrc
fi

if [ -f /etc/bash.bashrc ]; then
    . /etc/bash.bashrc
fi

export PATH=$PATH:/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin
export HOME=/home
export TERM=xterm-256color
export DEBIAN_FRONTEND=noninteractive
export TZ=UTC
export SHELL=$(command -v bash)
EOF
fi

# Check if clangd is installed, install if not
if ! command -v clangd > /dev/null 2>&1; then
    log_msg "[CLANGD] clangd not found, installing..."
    
    if [ "$DISTRO" = "alpine" ]; then
        run_silent apk add --no-cache clang-extra-tools || run_silent apk add --no-cache clang || true
        if ! command -v clangd > /dev/null 2>&1; then
            run_silent apk add --no-cache llvm || true
        fi
    else
        run_silent apt-get update -qq || true
        run_silent apt-get install -y clangd || run_silent apt-get install -y clang-tools || true
    fi
    
    # Final check
    if ! command -v clangd > /dev/null 2>&1; then
        log_msg "[CLANGD] ERROR: Failed to install clangd"
        exit 1
    fi
    
    log_msg "[CLANGD] Installation complete."
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    log_msg "[CLANGD] Install-only mode, exiting."
    exit 0
fi

# Run the LSP server
exec clangd --log=error --background-index --limit-results=10 --header-insertion=never --clang-tidy=false
