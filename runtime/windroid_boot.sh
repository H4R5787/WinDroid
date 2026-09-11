#!/bin/bash
# WinDroid Container Boot & Process Execution Script
# Invoked inside PRoot rootfs to launch Windows application via Wine & Box64

set -e

CONTAINER_PREFIX="${WINEPREFIX:-/root/wineprefix}"
TARGET_EXE="$1"
shift || true
EXTRA_ARGS="$@"

echo "[WinDroid] Initializing runtime environment..."
echo "[WinDroid] Wine Prefix: $CONTAINER_PREFIX"
echo "[WinDroid] Architecture: ${WINEARCH:-win64}"
echo "[WinDroid] Display: ${DISPLAY:-:0}"

# Verify pulse audio server
if command -v pulseaudio >/dev/null 2>&1; then
    if ! pgrep -x "pulseaudio" > /dev/null; then
        echo "[WinDroid] Starting PulseAudio daemon..."
        pulseaudio --start --exit-idle-time=-1 --load="module-native-protocol-tcp auth-anonymous=1 port=4713" || true
    fi
fi

# Ensure wine prefix exists
if [ ! -d "$CONTAINER_PREFIX" ]; then
    echo "[WinDroid] Bootstrapping fresh Wine prefix..."
    WINEDLLOVERRIDES="mscoree,mshtml=" wineboot -u || true
fi

# Set optimal Box64 settings if not already defined
export BOX64_DYNAREC="${BOX64_DYNAREC:-1}"
export BOX64_LOG="${BOX64_LOG:-1}"
export BOX64_NOBANNER="${BOX64_NOBANNER:-1}"

# Launch application
if [ -n "$TARGET_EXE" ]; then
    echo "[WinDroid] Launching application: $TARGET_EXE"
    exec wine "$TARGET_EXE" $EXTRA_ARGS
else
    echo "[WinDroid] No target executable supplied. Starting Wine Explorer..."
    exec wine explorer
fi
