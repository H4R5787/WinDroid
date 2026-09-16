#!/bin/bash
# WinDroid Container Boot & Process Orchestration Script
# Invoked inside PRoot rootfs to launch Windows application via Wine & Box64
# with hardware-accelerated Vulkan / DXVK / Turnip rendering pipeline.

set -e

CONTAINER_PREFIX="${WINEPREFIX:-/root/wineprefix}"
TARGET_EXE="$1"
shift || true
EXTRA_ARGS="$@"

echo "============================================================"
echo "         WinDroid Runtime Environment Initializing          "
echo "============================================================"
echo "[WinDroid] Container Prefix : $CONTAINER_PREFIX"
echo "[WinDroid] Architecture     : ${WINEARCH:-win64}"
echo "[WinDroid] Display Server   : ${DISPLAY:-:0}"
echo "[WinDroid] Graphics Driver  : ${WINDROID_GRAPHICS_DRIVER:-turnip}"

# 1. PulseAudio Audio Sink Daemon Initialization
if command -v pulseaudio >/dev/null 2>&1; then
    if ! pgrep -x "pulseaudio" > /dev/null; then
        echo "[WinDroid] Starting PulseAudio background daemon..."
        pulseaudio --start --exit-idle-time=-1 --load="module-native-protocol-tcp auth-anonymous=1 port=4713" || true
    else
        echo "[WinDroid] PulseAudio server already active on port 4713."
    fi
fi

# 2. Wine Prefix Verification & Initialization
if [ ! -d "$CONTAINER_PREFIX/drive_c" ]; then
    echo "[WinDroid] Bootstrapping fresh Wine prefix..."
    export WINEDLLOVERRIDES="mscoree,mshtml="
    wineboot -u || true
    echo "[WinDroid] Wine prefix bootstrap completed."
fi

# 3. Graphics ICD & DXVK Configuration
case "${WINDROID_GRAPHICS_DRIVER:-turnip}" in
    turnip)
        echo "[WinDroid] Configuring Mesa Turnip (Adreno Vulkan ICD)..."
        export VK_ICD_FILENAMES="/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json"
        export GALLIUM_DRIVER="zink"
        export MESA_LOADER_DRIVER_OVERRIDE="zink"
        export TU_DEBUG="noconform"
        ;;
    panfrost)
        echo "[WinDroid] Configuring Mesa Panfrost (Mali Vulkan ICD)..."
        export VK_ICD_FILENAMES="/usr/share/vulkan/icd.d/panfrost_icd.aarch64.json"
        export GALLIUM_DRIVER="panfrost"
        ;;
    virgl)
        echo "[WinDroid] Configuring VirGL OpenGL Virtualization..."
        export GALLIUM_DRIVER="virpipe"
        export MESA_GL_VERSION_OVERRIDE="3.3"
        ;;
    llvmpipe)
        echo "[WinDroid] Fallback: LLVMpipe CPU Software Rendering..."
        export LIBGL_ALWAYS_SOFTWARE=1
        export GALLIUM_DRIVER="llvmpipe"
        ;;
    *)
        echo "[WinDroid] Using system default Vulkan ICD..."
        ;;
esac

# 4. Direct3D -> Vulkan DLL Overrides (DXVK & VKD3D)
export WINEDLLOVERRIDES="${WINEDLLOVERRIDES:-d3d9,d3d10core,d3d11,dxgi=n,b;mscoree,mshtml=}"

# 5. Box64 Dynamic Recompiler Optimization Presets
export BOX64_DYNAREC="${BOX64_DYNAREC:-1}"
export BOX64_LOG="${BOX64_LOG:-1}"
export BOX64_NOBANNER="${BOX64_NOBANNER:-1}"
export BOX64_AVX="${BOX64_AVX:-1}"

# Performance flags
if [ -z "$BOX64_DYNAREC_FASTNAN" ]; then
    export BOX64_DYNAREC_FASTNAN=1
fi
if [ -z "$BOX64_DYNAREC_BIGBLOCK" ]; then
    export BOX64_DYNAREC_BIGBLOCK=1
fi
if [ -z "$BOX64_DYNAREC_SAFEFLAGS" ]; then
    export BOX64_DYNAREC_SAFEFLAGS=1
fi

echo "[WinDroid] Environment configuration ready."

# 6. Launch Application or Fallback to Wine Explorer
if [ -n "$TARGET_EXE" ]; then
    echo "[WinDroid] Executing target Windows binary: $TARGET_EXE"
    echo "[WinDroid] Arguments: $EXTRA_ARGS"
    exec wine "$TARGET_EXE" $EXTRA_ARGS
else
    echo "[WinDroid] No target executable supplied. Spawning Wine Explorer..."
    exec wine explorer
fi
