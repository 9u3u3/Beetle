#!/bin/sh
# proot-wrapper.sh - Keep proot environment running persistently

export LD_LIBRARY_PATH=$PREFIX
mkdir -p "$PREFIX/tmp"
mkdir -p "$PREFIX/alpine/tmp"
mkdir -p "$PREFIX/public"

export PROOT_TMP_DIR=$PREFIX/tmp

if [ -f "$NATIVE_DIR/libproot.so" ]; then
    export PROOT_LOADER="$NATIVE_DIR/libproot.so"
fi

if [ -f "$NATIVE_DIR/libproot32.so" ]; then
    export PROOT_LOADER32="$NATIVE_DIR/libproot32.so"
fi

if [ -e "$PREFIX/libtalloc.so.2" ] || [ -L "$PREFIX/libtalloc.so.2" ]; then
    rm "$PREFIX/libtalloc.so.2"
fi

ln -s "$NATIVE_DIR/libtalloc.so" "$PREFIX/libtalloc.so.2"
export PROOT="$NATIVE_DIR/libproot-xed.so"

ARGS="--kill-on-exit"

for system_mnt in /apex /odm /product /system /system_ext /vendor \
 /linkerconfig/ld.config.txt \
 /linkerconfig/com.android.art/ld.config.txt \
 /plat_property_contexts /property_contexts; do

 if [ -e "$system_mnt" ]; then
  system_mnt=$(realpath "$system_mnt")
  ARGS="$ARGS -b ${system_mnt}"
 fi
done
unset system_mnt

ARGS="$ARGS -b /sdcard"
ARGS="$ARGS -b /storage"
ARGS="$ARGS -b /dev"
ARGS="$ARGS -b /data"
ARGS="$ARGS -b /dev/urandom:/dev/random"
ARGS="$ARGS -b /proc"
ARGS="$ARGS -b /sys"
ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b $PREFIX/public:/public"
ARGS="$ARGS -b $PREFIX/alpine/tmp:/dev/shm"

if [ -e "/proc/self/fd" ]; then
  ARGS="$ARGS -b /proc/self/fd:/dev/fd"
fi

if [ -e "/proc/self/fd/0" ]; then
  ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin"
fi

if [ -e "/proc/self/fd/1" ]; then
  ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout"
fi

if [ -e "/proc/self/fd/2" ]; then
  ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr"
fi

ARGS="$ARGS -r $PREFIX/alpine"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

# Start proot with an interactive shell that reads from stdin
exec $PROOT $ARGS /bin/sh -c "
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin
export PS1=\"\[\e[38;5;46m\]\u\[\033[39m\]@localhost \[\033[39m\]\w \[\033[0m\]\\\\$ \"
export HOME=/home
export TERM=xterm-256color
cd /home
exec /bin/bash
"