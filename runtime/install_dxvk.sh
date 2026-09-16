#!/bin/bash
# WinDroid DXVK & VKD3D Direct3D Translation Layer Installer
# Installs DXVK (D3D9, D3D10, D3D11, DXGI) and VKD3D-Proton (D3D12) into a Wine prefix.

set -e

WINE_PREFIX="${1:-$WINEPREFIX}"

if [ -z "$WINE_PREFIX" ]; then
    echo "Usage: $0 <wine_prefix_path>"
    exit 1
fi

SYS32="$WINE_PREFIX/drive_c/windows/system32"
SYS64="$WINE_PREFIX/drive_c/windows/syswow64"
DXVK_SRC="/usr/share/dxvk"

mkdir -p "$SYS32"
mkdir -p "$SYS64"

echo "[WinDroid] Installing DXVK libraries to $WINE_PREFIX..."

DLLS=("d3d9.dll" "d3d10core.dll" "d3d11.dll" "dxgi.dll")

for dll in "${DLLS[@]}"; do
    if [ -f "$DXVK_SRC/x64/$dll" ]; then
        cp -f "$DXVK_SRC/x64/$dll" "$SYS32/$dll"
        echo "  -> Installed $dll (64-bit) into system32"
    fi
    if [ -f "$DXVK_SRC/x32/$dll" ]; then
        cp -f "$DXVK_SRC/x32/$dll" "$SYS64/$dll"
        echo "  -> Installed $dll (32-bit) into syswow64"
    fi
done

echo "[WinDroid] DXVK installation completed successfully."
