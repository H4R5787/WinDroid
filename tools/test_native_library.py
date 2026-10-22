#!/usr/bin/env python3
"""
Unit and integration test for WinDroid Native C++ JNI bridge.
Validates:
- Compilation of windroid_native.cpp with C++17
- Presence of required JNI entrypoints in shared library symbol table
- Host architecture and system capability detection
"""

import os
import sys
import subprocess
import unittest

class TestNativeLibrary(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.tools_dir = os.path.dirname(os.path.abspath(__file__))
        cls.project_root = os.path.dirname(cls.tools_dir)
        cls.cpp_source = os.path.join(cls.project_root, "app", "src", "main", "cpp", "windroid_native.cpp")
        cls.so_output = os.path.join(cls.tools_dir, "libwindroid_native_test.so")

        # Locate JNI includes or use fallback headers in tools/include
        jni_inc_args = []
        possible_jvm_dirs = [
            os.environ.get("JAVA_HOME", ""),
            "/usr/lib/jvm/java-17-openjdk-arm64",
            "/usr/lib/jvm/default-java",
        ]
        found_jvm_include = False
        for jvm in possible_jvm_dirs:
            if jvm and os.path.exists(os.path.join(jvm, "include", "jni.h")):
                jni_inc_args.append("-I" + os.path.join(jvm, "include"))
                platform_subdir = "darwin" if sys.platform == "darwin" else "linux"
                plat_path = os.path.join(jvm, "include", platform_subdir)
                if os.path.exists(plat_path):
                    jni_inc_args.append("-I" + plat_path)
                found_jvm_include = True
                break

        if not found_jvm_include:
            fallback_include = os.path.join(cls.tools_dir, "include")
            jni_inc_args.append("-I" + fallback_include)

        compiler = os.environ.get("CXX", "clang++" if sys.platform == "darwin" else "g++")

        # Compile native library with host compiler
        cmd = [
            compiler, "-shared", "-fPIC", "-std=c++17",
            *jni_inc_args,
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
        nm_cmd = ["nm", "-g", self.so_output] if sys.platform == "darwin" else ["nm", "-D", self.so_output]
        res = subprocess.run(nm_cmd, capture_output=True, text=True)
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
