#!/usr/bin/env python3
"""
Test suite for WinDroid Container Manager & Runtime Engine logic.
Validates:
- Container prefix creation and directory hierarchy
- dosdevices drive mapping (C:, D:)
- PRoot command-line synthesis
- Box64 Dynarec environment variable presets
- Container cleanup and deletion
"""

import os
import sys
import shutil
import tempfile
import unittest

class Box64Preset:
    SAFE = {
        "BOX64_DYNAREC": "1",
        "BOX64_DYNAREC_FASTNAN": "0",
        "BOX64_DYNAREC_FASTROUND": "0",
        "BOX64_DYNAREC_SAFEFLAGS": "1",
        "BOX64_DYNAREC_CALLRET": "0"
    }
    BALANCED = {
        "BOX64_DYNAREC": "1",
        "BOX64_DYNAREC_FASTNAN": "1",
        "BOX64_DYNAREC_FASTROUND": "0",
        "BOX64_DYNAREC_BIGBLOCK": "1",
        "BOX64_DYNAREC_SAFEFLAGS": "1",
        "BOX64_DYNAREC_CALLRET": "1"
    }
    AGGRESSIVE = {
        "BOX64_DYNAREC": "1",
        "BOX64_DYNAREC_FASTNAN": "1",
        "BOX64_DYNAREC_FASTROUND": "1",
        "BOX64_DYNAREC_BIGBLOCK": "2",
        "BOX64_DYNAREC_SAFEFLAGS": "0",
        "BOX64_DYNAREC_CALLRET": "1",
        "BOX64_DYNAREC_FORWARD": "256"
    }

class ContainerManagerMock:
    def __init__(self, base_dir: str):
        self.base_dir = base_dir
        self.containers_dir = os.path.join(base_dir, "containers")
        self.storage_dir = os.path.join(base_dir, "storage")
        os.makedirs(self.containers_dir, exist_ok=True)
        os.makedirs(self.storage_dir, exist_ok=True)

    def create_container(self, container_id: str, name: str, wine_arch: str = "win64", preset=Box64Preset.BALANCED):
        c_dir = os.path.join(self.containers_dir, container_id)
        prefix_dir = os.path.join(c_dir, ".wine")
        dosdevices = os.path.join(prefix_dir, "dosdevices")

        # Windows filesystem hierarchy
        drive_c = os.path.join(prefix_dir, "drive_c")
        os.makedirs(os.path.join(drive_c, "windows", "system32"), exist_ok=True)
        os.makedirs(os.path.join(drive_c, "windows", "syswow64"), exist_ok=True)
        os.makedirs(os.path.join(drive_c, "Program Files"), exist_ok=True)
        os.makedirs(os.path.join(drive_c, "Program Files (x86)"), exist_ok=True)
        os.makedirs(os.path.join(drive_c, "users", "windroid", "Desktop"), exist_ok=True)
        os.makedirs(dosdevices, exist_ok=True)

        # Drive C link
        c_link = os.path.join(dosdevices, "c:")
        if not os.path.exists(c_link):
            os.symlink("../drive_c", c_link)

        # Drive D (Games) link
        games_dir = os.path.join(self.storage_dir, "Games")
        os.makedirs(games_dir, exist_ok=True)
        d_link = os.path.join(dosdevices, "d:")
        if not os.path.exists(d_link):
            os.symlink(games_dir, d_link)

        return {
            "id": container_id,
            "name": name,
            "wine_arch": wine_arch,
            "prefix_dir": prefix_dir,
            "preset": preset,
            "drive_c": drive_c,
            "drive_d": games_dir
        }

    def delete_container(self, container_id: str):
        c_dir = os.path.join(self.containers_dir, container_id)
        if os.path.exists(c_dir):
            shutil.rmtree(c_dir)
            return True
        return False

    def build_proot_command(self, rootfs_dir: str, target_exe: str, container: dict, show_fps: bool = False):
        cmd = [
            "proot",
            "-0",
            "-r", rootfs_dir,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", self.base_dir,
            "-b", container["drive_d"],
            "-w", os.path.dirname(target_exe),
            "/usr/local/bin/box64",
            "/usr/local/bin/wine",
            target_exe
        ]

        env = {
            "WINEPREFIX": container["prefix_dir"],
            "WINEARCH": container["wine_arch"],
            "WINEDEBUG": "-all",
            "DISPLAY": ":0",
            "PULSE_SERVER": "127.0.0.1:4713",
            "DXVK_STATE_CACHE": "1",
            "GALLIUM_DRIVER": "zink"
        }
        if show_fps:
            env["DXVK_HUD"] = "fps,devinfo"
        env.update(container["preset"])
        return cmd, env


class TestContainerManager(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.mgr = ContainerManagerMock(self.temp_dir)

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def test_container_creation_and_filesystem(self):
        c = self.mgr.create_container("test-c1", "Test Container 1", "win64")
        self.assertTrue(os.path.isdir(c["drive_c"]))
        self.assertTrue(os.path.isdir(os.path.join(c["drive_c"], "windows", "system32")))
        self.assertTrue(os.path.isdir(os.path.join(c["drive_c"], "Program Files")))

        # Check dosdevices symlinks
        dosdevices = os.path.join(c["prefix_dir"], "dosdevices")
        c_link = os.path.join(dosdevices, "c:")
        d_link = os.path.join(dosdevices, "d:")
        self.assertTrue(os.path.islink(c_link))
        self.assertTrue(os.path.islink(d_link))

    def test_proot_command_assembly(self):
        c = self.mgr.create_container("test-c2", "Test Container 2", "win64", Box64Preset.AGGRESSIVE)
        cmd, env = self.mgr.build_proot_command(
            rootfs_dir="/root/rootfs",
            target_exe="/storage/Games/app.exe",
            container=c,
            show_fps=True
        )

        self.assertEqual(cmd[0], "proot")
        self.assertIn("-r", cmd)
        self.assertIn("/usr/local/bin/box64", cmd)
        self.assertIn("/usr/local/bin/wine", cmd)
        self.assertIn("/storage/Games/app.exe", cmd)

        # Check environment
        self.assertEqual(env["WINEARCH"], "win64")
        self.assertEqual(env["DXVK_HUD"], "fps,devinfo")
        self.assertEqual(env["BOX64_DYNAREC_FASTNAN"], "1")
        self.assertEqual(env["BOX64_DYNAREC_BIGBLOCK"], "2")
        self.assertEqual(env["BOX64_DYNAREC_FORWARD"], "256")

    def test_container_deletion(self):
        c = self.mgr.create_container("to-delete", "Disposable Container")
        c_dir = os.path.join(self.mgr.containers_dir, "to-delete")
        self.assertTrue(os.path.exists(c_dir))

        res = self.mgr.delete_container("to-delete")
        self.assertTrue(res)
        self.assertFalse(os.path.exists(c_dir))

if __name__ == "__main__":
    unittest.main()
