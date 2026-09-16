# WinDroid Build & Deployment Guide 🛠️

This document details the build prerequisites, compilation steps, native NDK integration, and release APK generation for **WinDroid**.

---

## 1. Prerequisites & Environment Setup

### Required SDK & Tools:
- **Android Studio** Hedgehog (2023.1.1) or newer / Command-line Tools
- **Android SDK Platform**: API Level 34 (Android 14)
- **Minimum SDK Supported**: API Level 28 (Android 9.0)
- **Android NDK**: Version 26.1.10909125 or newer
- **CMake**: 3.22.1+
- **JDK**: OpenJDK 17 or OpenJDK 21
- **Gradle**: 8.4+ with Android Gradle Plugin (AGP) 8.3.2

### Environment Variables:
Set `ANDROID_HOME` and `JAVA_HOME` in your shell profile (`~/.bashrc` or `~/.zshrc`):
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

---

## 2. Building the Project via Gradle

### A. Debug Build (Local Testing)
To assemble a debug APK for direct testing on connected Android devices or emulators:
```bash
cd /root/windroid
./gradlew assembleDebug
```
The output debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### B. Installing Directly to Device via ADB
Ensure USB debugging or Wireless debugging is enabled on your ARM64 Android device:
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. Building Native C++ JNI Bridge (`libwindroid_native.so`)

The native bridge handles POSIX pseudoterminal (PTY) spawning, Linux `memfd_create` shared memory buffers, and Vulkan hardware detection.

### Automatic Compilation via Gradle:
CMake is configured in `app/build.gradle.kts` under `externalNativeBuild`. Running `./gradlew assembleDebug` automatically invokes CMake and compiles `libwindroid_native.so` for `arm64-v8a`.

### Standalone Host Verification (Without Android NDK):
To test and verify compilation of `windroid_native.cpp` on an aarch64 Linux development host:
```bash
mkdir -p build_native && cd build_native
cmake ../app/src/main/cpp
cmake --build .
```

---

## 4. Building Production Release APK (Signed)

### Step 1: Generate Release Keystore (if not existing)
```bash
keytool -genkey -v \
  -keystore windroid-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias windroid \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

### Step 2: Configure Signing in `app/build.gradle.kts`
Add signing configuration to the `android` block:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../../windroid-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "YOUR_STORE_PASSWORD"
            keyAlias = "windroid"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### Step 3: Assemble Release APK
```bash
./gradlew assembleRelease
```
The optimized signed release APK will be located at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 5. Diagnostic & Subsystem Verification Harness

WinDroid provides an automated test suite verifying PE binary analysis, container isolation, application tracking, security policies, and native JNI linkage:

```bash
cd /root/windroid
python3 tools/run_all_tests.py
```

### Subsystem Verification Output:
```
============================================================
  RUNNING TEST SUITE: PE Parser & Compatibility Engine
--> PASSED
  RUNNING TEST SUITE: Container Manager & Sandboxing
--> PASSED
  RUNNING TEST SUITE: Application Manager & Persistence
--> PASSED
  RUNNING TEST SUITE: Security Policy & Sandboxing
--> PASSED
  RUNNING TEST SUITE: Native C++ JNI Bridge & Compilation
--> PASSED
  CHECKING HOST PROOT SANDBOX COMPATIBILITY
--> PASSED
============================================================
✓ ALL TESTS PASSED! WinDroid subsystems are functional.
```

---

## 6. Debugging & Diagnostic Telemetry

### Logcat Streaming
To inspect real-time logs from Wine, Box64, DXVK, and the native bridge:
```bash
adb logcat -s WinDroid WinDroidNative
```

### Wine Debug Channels
Fine-tune Wine logging channels by setting the `WINEDEBUG` environment variable inside Container Settings:
- Standard: `WINEDEBUG=-all` (fastest, silent)
- Relay trace: `WINEDEBUG=+relay`
- Direct3D issues: `WINEDEBUG=+d3d`

### Box64 Dynarec Debugging
- `BOX64_LOG=1`: Informational logs and CPU feature announcements
- `BOX64_LOG=2`: Verbose dynamic recompiler block trace
- `BOX64_DUMP=1`: Dump compiled machine code blocks to disk
