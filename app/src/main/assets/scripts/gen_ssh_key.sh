#!/bin/sh
# SSH ED25519 Key Generation Script for Alpine/proot
# This script generates ED25519 SSH keys for Git authentication

set -e

# Set PATH explicitly for Alpine
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin
export HOME=/home

# Create .ssh directory with proper permissions
mkdir -p /home/.ssh
chmod 700 /home/.ssh

# Install openssh if ssh-keygen not found
if ! command -v ssh-keygen > /dev/null 2>&1; then
    echo "Installing openssh..."
    apk update
    apk add openssh
fi

# Remove existing keys if present (for regeneration)
rm -f /home/.ssh/id_ed25519 /home/.ssh/id_ed25519.pub

# Generate ED25519 key pair (no passphrase)
ssh-keygen -t ed25519 -f /home/.ssh/id_ed25519 -N "" -C "bananaspaghetti@android"

# Verify keys were created and output the public key
if [ -f /home/.ssh/id_ed25519 ] && [ -f /home/.ssh/id_ed25519.pub ]; then
    echo "SUCCESS: Keys generated"
    cat /home/.ssh/id_ed25519.pub
else
    echo "FAILED: Keys not found"
    exit 1
fi
