# WinDroid 🎮🚀

**WinDroid** is an open-source Android-native application that enables compatible Windows (`.exe` and `.msi`) software to run directly on ARM64 Android devices without requiring a Windows operating system, dual boot, virtual machine, or remote Windows PC.

Conceptually modeled after high-performance translation runtimes such as Winlator and Proton, WinDroid orchestrates **Wine**, **Box64/Box86 dynamic binary translation**, **DXVK**, and **native Android Vulkan WSI** to translate Windows APIs and x86/x86_64 instructions into native Linux/Android ARM64 system calls and Vulkan command buffers.

---

## ⚙️ Architectural Pipeline

```
+-------------------------------------------------------------+
|               Android Host (Kotlin / Jetpack Compose)       |
|   - Scoped Storage (SAF)       - Application Manager (Apps) |
|   - Container Manager (Prefix) - PE32/PE32+ Binary Scanner  |
|   - Virtual Gamepad Overlay    - Performance Telemetry HUD  |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|               Native C++ JNI Bridge (CMake / NDK)           |
|   - PTY Pseudoterminal Session - Linux memfd Shared Memory  |
|   - Fast Input Event Queue     - Vulkan Driver Capability   |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|              PRoot Sandboxed Linux Runtime (aarch64)        |
|   - Isolated Filesystem Root   - PulseAudio Daemon (4713)   |
|   - Safe Mount Namespaces      - X11 / Wayland Compositor   |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|               Box64 (x86_64) / Box86 (x86 32-bit)           |
|   - Dynamic Binary Translation / JIT Recompiler             |
|   - FastNaN, FastRound, BigBlock, SafeFlags Tuning Presets  |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                       Wine Subsystem                        |
|   - Wine-GE 8.26 WoW64         - Win32/Win64 Core APIs      |
|   - Virtual Drives (C:, D:)    - Registry & Process Tree    |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|               Graphics & Audio Translation Layer            |
|   - DXVK (Direct3D 9/10/11 -> Vulkan Command Buffers)       |
|   - VKD3D-Proton (Direct3D 12 -> Vulkan)                    |
|   - Mesa Turnip (Snapdragon Adreno) / System Native Vulkan  |
|   - Low-latency PulseAudio -> Android AAudio / Oboe Output  |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                     Windows Executable (.exe)               |
+-------------------------------------------------------------+
```

---

## 🌟 Core Subsystems & Features

### 1. Portable Executable (PE) Inspector
- Parses Windows PE32 (x86) and PE32+ (x86_64) headers in real time.
- Resolves bitness, subsystem (GUI vs Console), and dynamic import tables (`IMAGE_IMPORT_DESCRIPTOR`).
- Detects imported graphics APIs (`d3d9.dll`, `d3d11.dll`, `d3d12.dll`, `vulkan-1.dll`, `opengl32.dll`) and automatically recommends the best graphics backend.

### 2. Multi-Container Management & Scoped Storage
- Isolated container environments stored under `/Android/data/org.windroid/files/containers/<id>/.wine`.
- Safe virtual drive mappings via Wine `dosdevices/`:
  - `C:` -> Container private system drive (`drive_c/windows`, `Program Files`).
  - `D:` -> User-selected game directory via Android Storage Access Framework (SAF).
  - `Z:` -> Read-only sandboxed rootfs libraries.
- Zero unnecessary permissions; complies with Android 14 Scoped Storage.

### 3. Application Manager & Library
- Track installed Windows applications, game shortcuts, and playtime statistics.
- Customize individual per-app settings (resolution, Box64 preset, graphics backend, launch arguments).

### 4. Dynamic Binary Translation (Box64 / Box86)
- **Safe Preset**: Maximum compatibility with strict floating point semantics (`BOX64_DYNAREC_SAFEFLAGS=1`).
- **Balanced Preset**: Recommended for general gaming with fast NaN checks and BigBlock caching.
- **Aggressive Preset**: Peak FPS with unaligned access optimizations and instruction unrolling.

### 5. Hardware-Accelerated Graphics Pipeline
- **Mesa Turnip**: Direct hardware Vulkan ICD for Qualcomm Snapdragon (Adreno 6xx/7xx/8xx) with GMEM tile acceleration.
- **System Vulkan**: Native device Vulkan ICD (`libvulkan.so`) for MediaTek, Exynos, and Tensor GPUs.
- **Mesa Panfrost**: Mali GPU Vulkan acceleration.
- **VirGL & LLVMpipe**: Virtualized OpenGL and CPU software fallbacks.
- **DXVK 2.3.1**: Translates Direct3D 9/10/11 calls directly to Vulkan command buffers with async pipeline compilation.

### 6. Comprehensive Input System
- **Relative Trackpad**: Accelerated mouse cursor navigation with two-finger right-click.
- **Direct Touchscreen**: 1:1 touch coordinate mapping for strategy and menu navigation.
- **Virtual Gamepad Overlay**: On-screen D-Pad, dual analog thumbsticks, and ABXY / shoulder buttons with customizable opacity.
- **Physical Gamepads**: Automatic pass-through of Bluetooth and USB HID controllers to Windows XInput.

### 7. Security Policy & Sandboxing
- Pre-execution risk assessment scoring untrusted binaries.
- SHA-256 fingerprint verification and quarantine warnings for direct internet downloads.
- Path traversal protection preventing container escapes into private Android storage.

---

## 📱 User Interface Modules

WinDroid features a modern, handheld-optimized Android UI built with Jetpack Compose:

| Screen | Description |
|---|---|
| **Home** | Dashboard with telemetry stats, quick-launch hero card, and recent game library. |
| **Add Windows Application** | 5-step wizard to browse `.exe` files, select container, and configure presets. |
| **Installed Applications** | Game library view with search, category filters, playtime tracking, and shortcuts. |
| **Containers** | List and create isolated Wine containers with custom prefixes and architectures. |
| **Container Editor** | Multi-tab container customization: general, graphics driver, Box64 tuning, and drives. |
| **Runtime Manager** | Real-time status of PRoot rootfs, Box64/Box86 JIT, Wine-GE, and DXVK libraries. |
| **Graphics Settings** | Driver selection (Turnip / System Vulkan), DXVK HUD toggles, and resolutions. |
| **Input Settings** | Mouse mode selector, touch sensitivity slider, and virtual gamepad configuration. |
| **Performance Monitor** | Live telemetry HUD: FPS, frametimes, RAM allocation, CPU load, and GPU status. |
| **Diagnostic Logs** | Real-time log stream with color-coded filters (`WINE`, `BOX64`, `DXVK`, `SYSTEM`). |
| **About** | App version, hardware profile, and open-source license compliance. |

---

## 📁 Repository Structure

```
windroid/
├── app/
│   ├── build.gradle.kts                   # Android Gradle build & NDK CMake linkage
│   └── src/main/
│       ├── AndroidManifest.xml            # Permissions, file associations (.exe, .msi)
│       ├── cpp/                           # Native C++ JNI bridge
│       │   ├── CMakeLists.txt             # Native library build definition
│       │   ├── windroid_native.h          # Hardware info & JNI exported declarations
│       │   └── windroid_native.cpp        # PTY spawning, memfd buffers, Vulkan probe
│       ├── java/org/windroid/
│       │   ├── MainActivity.kt            # Jetpack Compose Navigation & app lifecycle
│       │   ├── WinDroidApplication.kt     # Subsystem singleton initialization
│       │   ├── core/
│       │   │   ├── application/           # App manager, shortcuts & playtime tracker
│       │   │   ├── container/             # Prefix isolation, drive mapping, Box64 presets
│       │   │   ├── graphics/              # Driver backends (Turnip, System Vulkan, VirGL)
│       │   │   ├── input/                 # Mouse modes & virtual gamepad definitions
│       │   │   ├── nativebridge/          # JNI bridge to libwindroid_native.so
│       │   │   ├── pe/                    # PE32/PE32+ binary parser & DirectX detector
│       │   │   ├── runtime/               # PRoot orchestration, Wine execution & rootfs
│       │   │   └── security/              # Risk evaluation, checksums & sandbox checks
│       │   └── ui/
│       │       ├── components/            # Virtual Gamepad Overlay & In-Game Quick Menu
│       │       ├── screens/               # 10 Compose UI module screens
│       │       └── theme/                 # Dark gaming handheld theme
│       └── res/                           # Android resources & strings
├── runtime/
│   ├── windroid_boot.sh                   # Container boot, driver init & wine launcher
│   └── install_dxvk.sh                    # DXVK/VKD3D DLL installation script
├── tools/
│   ├── pe_scanner.py                      # Standalone CLI PE inspector
│   ├── test_pe_scanner.py                 # PE parser test suite
│   ├── test_container_manager.py          # Container isolation test suite
│   ├── test_application_manager.py        # App manager & persistence test suite
│   ├── test_security_manager.py           # Security sandbox & risk test suite
│   ├── test_native_library.py             # Native C++ compilation & JNI symbol test suite
│   └── run_all_tests.py                   # Master test runner
├── BUILD.md                               # Comprehensive build & release APK guide
├── ARCHITECTURE.md                        # Deep technical architecture documentation
├── build.gradle.kts                       # Root project build configuration
└── settings.gradle.kts                    # Gradle settings & plugin repositories
```

---

## 🧪 Testing & Verification

Execute the complete automated test suite on your development machine:
```bash
python3 tools/run_all_tests.py
```
All 6 subsystem test suites will run and report diagnostic results.

---

## 📜 Open-Source Compliance & Legal Attribution

WinDroid does **not** bundle, distribute, or require proprietary Microsoft Windows binary files. All Windows APIs are implemented through clean-room open-source projects:

- **Wine**: GNU Lesser General Public License (LGPL) v2.1+
- **Box64 & Box86**: MIT License (ptitSeb)
- **DXVK**: zlib / libpng License (doitsujin)
- **VKD3D-Proton**: GNU Lesser General Public License (LGPL) v2.1
- **Mesa Turnip / Panfrost**: MIT / X11 License (Mesa3D Project)
- **PRoot**: GNU General Public License (GPL) v2.0
