# WinDroid 🎮🚀

**WinDroid** is an Android-native application that enables compatible Windows (`.exe` and `.msi`) software to run directly on ARM64 Android devices without installing Windows, virtual machines, or remote PCs.

Using open-source technologies such as **Wine**, **Box64/Box86**, **Vulkan**, **DXVK**, and API translation layers, WinDroid provides a secure, sandboxed, and hardware-accelerated runtime environment for Windows applications.

---

## ⚙️ Architecture

```
+-------------------------------------------------------------+
|               Android Host (Kotlin / Jetpack Compose)       |
|   - Scoped Storage (SAF)       - PE Binary Inspector        |
|   - Container Manager (Prefixes) - SurfaceView & Vulkan WSI |
|   - Virtual Gamepad / Touch    - Oboe / AAudio Pipeline     |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|              PRoot Sandboxed Linux Runtime (aarch64)        |
|   - Isolated Filesystem Root   - PulseAudio Daemon          |
|   - Syscall Translation        - X11 / Wayland Compositor   |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|               Box64 (x86_64) / Box86 (x86 32-bit)           |
|   - Dynamic Binary Translation / JIT Recompiler             |
|   - Fast Floating Point & Dynarec Optimization Presets      |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                       Wine Subsystem                        |
|   - Wine Server & WoW64        - Win32/Win64 Core APIs      |
|   - Registry & Virtual Drives  - Process Management         |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|               Graphics & Audio Translation Layer            |
|   - DXVK (Direct3D 9/10/11 -> Vulkan)                       |
|   - VKD3D-Proton (Direct3D 12 -> Vulkan)                    |
|   - Mesa Turnip (Adreno Vulkan) / Panfrost / System Vulkan  |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                     Windows Executable                      |
+-------------------------------------------------------------+
```

---

## 🌟 Key Features

1. **Direct Binary Execution**: Run `.exe` and `.msi` installers and games directly on your device.
2. **PE Binary Inspector**: Inspects PE headers to auto-detect bitness (32-bit vs 64-bit), subsystem (GUI vs Console), and graphics requirements (DirectX 9/11/12, Vulkan, OpenGL).
3. **Isolated Containers**: Create multiple isolated containers with custom Wine prefixes, resolutions, driver setups, and Box64 presets.
4. **Sandboxed File Access**: Uses Android Storage Access Framework (SAF) and mapped drives (`C:`, `D:`) to prevent unauthorized file system access.
5. **Vulkan & DXVK Acceleration**: Translate DirectX 9, 10, 11, and 12 calls to native Vulkan with optional Turnip Adreno drivers for peak GPU performance.
6. **Input Management**: Relative trackpad mouse emulation, direct touch, on-screen virtual gamepads, and physical Bluetooth/USB controller pass-through.
7. **Real-time Diagnostic Logs**: Monitor Wine, Box64, and DXVK logs live inside the app to identify and fix compatibility issues.

---

## 📁 Repository Structure

```
windroid/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/org/windroid/
│           ├── MainActivity.kt
│           ├── WinDroidApplication.kt
│           ├── core/
│           │   ├── pe/              # PE32/PE32+ parser & compatibility engine
│           │   ├── container/       # Prefix isolation & drive mapping
│           │   ├── runtime/         # PRoot / Wine / Box64 execution engine
│           │   ├── graphics/        # Graphics drivers & DXVK configuration
│           │   └── input/           # Touch & gamepad mapping
│           └── ui/
│               ├── screens/         # Compose UI screens
│               └── theme/           # Dark gaming UI theme
├── runtime/
│   └── windroid_boot.sh             # Container initialization & wine startup script
├── tools/
│   ├── pe_scanner.py                # Standalone PE inspector CLI
│   └── test_pe_scanner.py           # Unit tests for binary inspector
├── build.gradle.kts
├── settings.gradle.kts
└── ARCHITECTURE.md
```

---

## 🚀 Incremental Roadmap

- **Phase 1: Foundation & File Selection** *(Current)*: PE header parser, compatibility detector, container manager, Jetpack Compose UI.
- **Phase 2: Runtime Rootfs & Emulation**: Minimal aarch64 rootfs deployment, PRoot execution bridge, Box64/Box86 JIT compilation.
- **Phase 3: Display & Audio Integration**: SurfaceView Vulkan swapchain, X11/Wayland presentation bridge, PulseAudio to AAudio low-latency output.
- **Phase 4: Hardware Graphics Acceleration**: DXVK/VKD3D integration, Adreno Turnip driver switching, resolution scaling & FSR.
- **Phase 5: Input Profiles & Release**: Gamepad profiles, on-screen custom button editor, automated CI packaging.

---

## 📜 Open-Source Compliance & Licenses

WinDroid integrates open-source projects adhering strictly to their respective licenses:
- **Wine**: LGPL 2.1+
- **Box64 / Box86**: MIT License
- **DXVK / VKD3D-Proton**: zlib / LGPL 2.1
- **Mesa Turnip**: MIT / X11
- **PRoot**: GPL 2.0
