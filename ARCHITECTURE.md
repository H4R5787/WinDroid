# WinDroid Technical Architecture

## 1. Execution Flow & Subsystem Bridges

When a user selects a `.exe` or `.msi` file in WinDroid:

1. **PE Inspection Phase**:
   - The file is read via `PeParser.kt`.
   - `PeParser` reads the DOS header (`IMAGE_DOS_HEADER`), resolves `e_lfanew`, verifies the `PE\0\0` signature, and reads the COFF & Optional Headers.
   - The machine architecture determines the translation strategy:
     - `IMAGE_FILE_MACHINE_I386` (0x014C): Uses **Box86** and **win32** Wine prefix.
     - `IMAGE_FILE_MACHINE_AMD64` (0x8664): Uses **Box64** and **win64** Wine prefix.
     - `IMAGE_FILE_MACHINE_ARM64` (0xAA64): Executes directly without binary translation.
   - Section headers and the Import Directory Table (`IMAGE_IMPORT_DESCRIPTOR`) are traversed:
     - Detects imported DLLs (`d3d9.dll`, `d3d11.dll`, `d3d12.dll`, `opengl32.dll`, `vulkan-1.dll`).
     - Recommends the optimal graphics backend (e.g. DXVK for D3D9-11, VKD3D for D3D12).

2. **Container Isolation & Drive Mapping**:
   - Each container resides in an isolated directory: `$APP_FILES/containers/<container_id>/.wine`.
   - Virtual drive mapping is handled in `dosdevices/`:
     - `c:` -> `$WINEPREFIX/drive_c`
     - `d:` -> Mapped Android SAF directory (e.g., user Games folder).
     - `z:` -> Sandboxed rootfs libraries (read-only).
   - This ensures full compliance with Android Scoped Storage and prevents unauthorized access to device data.

3. **PRoot & Dynamic Binary Translation**:
   - The execution command is constructed by `WineRuntimeEngine.kt`:
     `proot -0 -r $ROOTFS -b /dev -b /proc -b /sys -b $APP_FILES -w $APP_DIR /usr/local/bin/box64 /usr/local/bin/wine target.exe`
   - `BOX64_DYNAREC` JIT-recompiles x86_64 instructions into ARM64 instructions at runtime.
   - Dynarec presets control optimizations such as FastNaN, FastRound, and BigBlock caching.

4. **Graphics & Audio Presentation**:
   - Windows Direct3D calls are intercepted by DXVK and translated into Vulkan command buffers.
   - Vulkan frames are rendered using Turnip (for Qualcomm Adreno GPUs) or System Vulkan ICD.
   - The swapchain presents frames to an Android `SurfaceView` via `AHardwareBuffer` or X11 shared memory buffer.
   - Audio is routed via PulseAudio to Android AAudio/Oboe for minimal latency.
