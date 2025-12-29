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

if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

# Determine initrc path based on distro
if [ "$DISTRO" = "alpine" ]; then
    INITRC_PATH="$PREFIX/alpine/initrc"
else
    INITRC_PATH="$PREFIX/ubuntu/initrc"
fi

# Create initrc if it doesn't exist
if [ ! -e "$INITRC_PATH" ]; then
    cat <<EOF > "$INITRC_PATH"
if [ -f ~/.bashrc ]; then
    . ~/.bashrc
fi

if [ -f /etc/bash.bashrc ]; then
    . /etc/bash.bashrc
fi

export PATH=\$PATH:/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin
export HOME=/home
export TERM=xterm-256color
export SHELL=\$(command -v bash)

EOF
fi

# Check if clangd is installed, install if not
if ! command -v clangd > /dev/null 2>&1; then
    echo "[CLANGD] clangd not found, installing..." >&2
    
    if [ "$DISTRO" = "alpine" ]; then
        # Install clangd via apk (Alpine Linux)
        apk add --no-cache clang-extra-tools 2>&1 >&2 || apk add --no-cache clang 2>&1 >&2 || true
        # Fallback: try installing llvm which includes clangd
        if ! command -v clangd > /dev/null 2>&1; then
            apk add --no-cache llvm 2>&1 >&2 || true
        fi
    else
        # Ubuntu: install clangd
        apt-get update -qq 2>&1 >&2 || true
        apt-get install -y clangd 2>&1 >&2 || apt-get install -y clang-tools 2>&1 >&2 || true
    fi
    
    # Final check
    if ! command -v clangd > /dev/null 2>&1; then
        echo "[CLANGD] ERROR: Failed to install clangd" >&2
        exit 1
    fi
    
    echo "[CLANGD] Installation complete." >&2
fi

# If install-only mode, exit now without starting LSP
if [ "$INSTALL_ONLY" = true ]; then
    echo "[CLANGD] Install-only mode, exiting." >&2
    exit 0
fi

# Run the LSP server
exec clangd --log=error --background-index --limit-results=10 --header-insertion=never --clang-tidy=false
