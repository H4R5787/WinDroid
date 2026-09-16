#!/usr/bin/env python3
"""
Unit test suite for WinDroid Security & Sandboxing Policy.
Tests:
- Executable risk assessment (Downloads folder warning, corrupted binaries, driver blocking)
- SHA-256 binary checksum computation
- Storage path containment & path traversal escape prevention (../../)
- Container drive mapping isolation
"""

import hashlib
import os
import shutil
import tempfile
import unittest

class SecurityPolicyMock:
    def __init__(self, base_dir: str):
        self.base_dir = os.path.realpath(base_dir)

    def calculate_sha256(self, filepath: str) -> str:
        if not os.path.exists(filepath):
            return "UNKNOWN"
        h = hashlib.sha256()
        with open(filepath, "rb") as f:
            while chunk := f.read(8192):
                h.update(chunk)
        return h.hexdigest()

    def assess_risk(self, filepath: str, is_valid_pe: bool, subsystem: str) -> dict:
        warnings = []
        risk_level = "LOW"

        if not os.path.exists(filepath):
            return {"safe": False, "risk": "HIGH", "warnings": ["File does not exist"]}

        # Check downloads
        if "download" in filepath.lower():
            warnings.append("File located in Downloads folder.")
            risk_level = "MEDIUM"

        # Check PE validity
        if not is_valid_pe:
            warnings.append("Malformed or invalid PE header.")
            risk_level = "HIGH"

        # Check driver subsystem
        if "driver" in subsystem.lower() or "native" in subsystem.lower():
            warnings.append("Kernel-mode driver blocked.")
            risk_level = "HIGH"

        # Protected names
        filename = os.path.basename(filepath).lower()
        if filename in ["ntdll.dll", "kernel32.dll", "wineboot.exe"]:
            warnings.append("Protected Wine subsystem component name.")
            risk_level = "HIGH"

        return {
            "safe": risk_level != "HIGH",
            "risk": risk_level,
            "requires_consent": risk_level != "LOW",
            "warnings": warnings,
            "sha256": self.calculate_sha256(filepath)
        }

    def validate_path_isolation(self, path: str, allowed_dirs: list) -> bool:
        try:
            target = os.path.realpath(path)
            for allowed in allowed_dirs:
                allowed_real = os.path.realpath(allowed)
                if target.startswith(allowed_real):
                    return True
            return False
        except Exception:
            return False


class TestSecurityManager(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.sec = SecurityPolicyMock(self.temp_dir)

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def test_sha256_checksum(self):
        test_file = os.path.join(self.temp_dir, "test.bin")
        data = b"WinDroid Execution Security Test"
        with open(test_file, "wb") as f:
            f.write(data)

        expected = hashlib.sha256(data).hexdigest()
        self.assertEqual(self.sec.calculate_sha256(test_file), expected)

    def test_downloads_folder_risk(self):
        dl_dir = os.path.join(self.temp_dir, "Downloads")
        os.makedirs(dl_dir, exist_ok=True)
        exe = os.path.join(dl_dir, "game.exe")
        with open(exe, "wb") as f:
            f.write(b"MZ" + b"\x00" * 100)

        assessment = self.sec.assess_risk(exe, is_valid_pe=True, subsystem="GUI")
        self.assertTrue(assessment["safe"])
        self.assertEqual(assessment["risk"], "MEDIUM")
        self.assertTrue(assessment["requires_consent"])
        self.assertIn("Downloads", assessment["warnings"][0])

    def test_kernel_driver_blocked(self):
        driver_file = os.path.join(self.temp_dir, "vboxdrv.sys")
        with open(driver_file, "wb") as f:
            f.write(b"MZ" + b"\x00" * 100)

        assessment = self.sec.assess_risk(driver_file, is_valid_pe=True, subsystem="Native NT Driver")
        self.assertFalse(assessment["safe"])
        self.assertEqual(assessment["risk"], "HIGH")

    def test_path_isolation_and_traversal_prevention(self):
        container_prefix = os.path.join(self.temp_dir, "containers", "c1", "drive_c")
        games_dir = os.path.join(self.temp_dir, "storage", "Games")
        os.makedirs(container_prefix, exist_ok=True)
        os.makedirs(games_dir, exist_ok=True)

        allowed = [container_prefix, games_dir]

        # Valid in container
        valid_path = os.path.join(container_prefix, "Program Files", "game.exe")
        self.assertTrue(self.sec.validate_path_isolation(valid_path, allowed))

        # Path traversal attempt (/../..)
        traversal_attack = os.path.join(container_prefix, "..", "..", "..", "etc", "passwd")
        self.assertFalse(self.sec.validate_path_isolation(traversal_attack, allowed))

        # Root access attempt
        self.assertFalse(self.sec.validate_path_isolation("/data/data/com.windroid/databases", allowed))

if __name__ == "__main__":
    unittest.main()
