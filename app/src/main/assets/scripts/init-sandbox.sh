export LD_LIBRARY_PATH=$PREFIX # Done

mkdir -p "$PREFIX/tmp" # Done
mkdir -p "$PREFIX/alpine/tmp" # Done
mkdir -p "$PREFIX/public" # Done

export PROOT_TMP_DIR=$PREFIX/tmp # Done

if [ -f "$NATIVE_DIR/libproot.so" ]; then
    export PROOT_LOADER="$NATIVE_DIR/libproot.so"
fi # Done

if [ -f "$NATIVE_DIR/libproot32.so" ]; then
    export PROOT_LOADER32="$NATIVE_DIR/libproot32.so"
fi # Done

if [ -e "$PREFIX/libtalloc.so.2" ] || [ -L "$PREFIX/libtalloc.so.2" ]; then
    rm "$PREFIX/libtalloc.so.2"
fi # Done

ln -s "$NATIVE_DIR/libtalloc.so" "$PREFIX/libtalloc.so.2" # Done
export PROOT="$NATIVE_DIR/libproot-xed.so" # Done

ARGS="--kill-on-exit" # Done

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

$PROOT $ARGS /bin/sh $PREFIX/init-alpine.sh "$@"

# Changes made :
# Removed the FDroid part, as it is not needed (removed the whole if block and just made the normal way)
# If anything doesn't work with this method just use the FDroid method