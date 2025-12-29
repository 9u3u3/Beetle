set -e  # Exit immediately on failure

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin
export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@localhost \[\033[39m\]\w \[\033[0m\]\\$ "
export HOME=/home
export TERM=xterm-256color

# Setup apt sources.list if missing (base rootfs doesn't include it)
if [ ! -f /etc/apt/sources.list ] || [ ! -s /etc/apt/sources.list ]; then
    echo "Setting up apt sources..."
    cat > /etc/apt/sources.list << 'SOURCES'
deb http://ports.ubuntu.com/ubuntu-ports noble main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports noble-updates main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports noble-security main restricted universe multiverse
SOURCES
fi

# Check if bash is installed, if not install it
if ! command -v bash > /dev/null 2>&1; then
    echo "Installing bash..."
    apt-get update && apt-get install -y bash
    if [ $? -eq 0 ]; then
        echo -e "\e[32;1m[+] \e[0mSuccessfully installed bash\e[0m"
    fi
fi

if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi


if [ "$1" = "--installing" ]; then
    mkdir -p "$PREFIX/.configured"
    echo "Installation completed."
    exit 0
fi

if [ "$#" -eq 0 ]; then
    echo "$$" > "$PREFIX/pid"

    # Create initrc if it doesn't exist
    if [ ! -e "$PREFIX/ubuntu/initrc" ]; then
        cat <<EOF > "$PREFIX/ubuntu/initrc"
if [[ -f ~/.bashrc ]]; then
    source ~/.bashrc
fi

if [[ -f /etc/bash.bashrc ]]; then
    source /etc/bash.bashrc
fi

export PATH=\$PATH:/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin
export HOME=/home
export TERM=xterm-256color
export SHELL=\$(command -v bash)

# Start in home directory
cd

EOF
    fi

    echo 'export PS1="\[\e[38;5;46m\]\u\[\e[0m\]@localhost \[\e[0m\]\w \[\e[0m\]\\$ "' >> $PREFIX/ubuntu/initrc
    echo 'cd' >> $PREFIX/ubuntu/initrc
    chmod +x "$PREFIX/ubuntu/initrc"
    exec /bin/bash --rcfile /data/user/0/com.bsbeetle.bananaspaghetti/files/ubuntu/initrc -i

else
    exec "$@"
fi
