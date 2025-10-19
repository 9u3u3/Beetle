set -e  # Exit immediately on failure

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin
export HOME=/home
export TERM=xterm-256color

if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

if [ "$#" -eq 0 ]; then
    echo "$$" > "$PREFIX/pid"

    # Create initrc if it doesn't exist
    if [ ! -e "$PREFIX/alpine/initrc" ]; then
        cat <<EOF > "$PREFIX/alpine/initrc"
if [[ -f ~/.bashrc ]]; then
    source ~/.bashrc
fi

if [[ -f /etc/bash/bashrc ]]; then
    source /etc/bash/bashrc
fi

export PATH=\$PATH:/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin
export HOME=/home
export TERM=xterm-256color
export SHELL=\$(command -v bash)

EOF
    fi
    exec "clangd"
else
    exec "$@"
fi