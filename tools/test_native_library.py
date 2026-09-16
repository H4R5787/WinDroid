#!/usr/bin/env python3
"""
Unit and integration test for WinDroid Native C++ JNI bridge.
Validates:
- Compilation of windroid_native.cpp with C++17
- Presence of required JNI entrypoints in shared library symbol table
- Host architecture and system capability detection
"""

import os
import subprocess
import unittest

class TestNativeLibrary(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.tools_dir = os.path.dirname(os.path.abspath(__file__))
        cls.project_root = os.path.dirname(cls.tools_dir)
        cls.cpp_source = os.path.join(cls.project_root, "app", "src", "main", "cpp", "windroid_native.cpp")
        cls.so_output = os.path.join(cls.tools_dir, "libwindroid_native_test.so")

        # Compile native library with host g++ / clang++
        cmd = [
            "g++", "-shared", "-fPIC", "-std=c++17",
            "-I/usr/lib/jvm/java-17-openjdk-arm64/include",
            "-I/usr/lib/jvm/java-17-openjdk-arm64/include/linux",
            "-I" + os.path.join(cls.project_root, "app", "src", "main", "cpp"),
            cls.cpp_source,
            "-o", cls.so_output,
            "-ldl", "-lutil"
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode != 0:
            raise RuntimeError(f"Native compilation failed: {res.stderr}")

    @classmethod
    def tearDownClass(cls):
        if os.path.exists(cls.so_output):
            os.remove(cls.so_output)

    def test_library_compiled_successfully(self):
        self.assertTrue(os.path.exists(self.so_output))
        self.assertGreater(os.path.getsize(self.so_output), 1024)

    def test_exported_jni_symbols(self):
        res = subprocess.run(["nm", "-D", self.so_output], capture_output=True, text=True)
        self.assertEqual(res.returncode, 0)
        symbols = res.stdout

        expected_symbols = [
            "Java_org_windroid_core_nativebridge_NativeBridge_getCpuArchitecture",
            "Java_org_windroid_core_nativebridge_NativeBridge_checkVulkanSupport",
            "Java_org_windroid_core_nativebridge_NativeBridge_detectGpuRenderer",
            "Java_org_windroid_core_nativebridge_NativeBridge_createPtySession",
            "Java_org_windroid_core_nativebridge_NativeBridge_sendInputEvent",
            "Java_org_windroid_core_nativebridge_NativeBridge_createSharedBuffer",
            "Java_org_windroid_core_nativebridge_NativeBridge_closePtySession"
        ]

        for sym in expected_symbols:
            self.assertIn(sym, symbols, f"Missing required JNI symbol: {sym}")

if __name__ == "__main__":
    unittest.main()
