#!/usr/bin/env python3
"""
Unit test suite for WinDroid Application Manager & Shortcut Tracking.
Tests:
- Application shortcut registration
- Per-application configuration persistence (resolution, graphics backend, Box64 preset)
- Playtime accumulation & last played timestamp tracking
- JSON serialization / deserialization roundtrip
"""

import json
import os
import shutil
import tempfile
import unittest

class ApplicationManagerMock:
    def __init__(self, base_dir: str):
        self.base_dir = base_dir
        self.apps_file = os.path.join(base_dir, "installed_apps.json")
        self.installed_apps = []
        self.load()

    def register(self, app_id: str, title: str, exe_path: str, container_id: str,
                 arch: str = "x86_64", graphics: str = "TURNIP_ADRENO",
                 resolution: str = "1280x720", preset: str = "BALANCED", category: str = "Games"):
        app = {
            "id": app_id,
            "title": title,
            "exePath": exe_path,
            "arguments": "",
            "containerId": container_id,
            "architecture": arch,
            "graphicsBackend": graphics,
            "resolution": resolution,
            "box64Preset": preset,
            "dxvkEnabled": True,
            "vkd3dEnabled": False,
            "showFpsCounter": False,
            "installTimestamp": 1726000000000,
            "lastPlayedTimestamp": 0,
            "totalPlayTimeSeconds": 0,
            "category": category
        }
        self.installed_apps = [a for a in self.installed_apps if a["id"] != app_id and a["exePath"] != exe_path]
        self.installed_apps.insert(0, app)
        self.save()
        return app

    def record_playtime(self, app_id: str, seconds: int):
        for app in self.installed_apps:
            if app["id"] == app_id:
                app["totalPlayTimeSeconds"] += seconds
                app["lastPlayedTimestamp"] = 1726500000000
                self.save()
                return True
        return False

    def remove(self, app_id: str):
        before = len(self.installed_apps)
        self.installed_apps = [a for a in self.installed_apps if a["id"] != app_id]
        if len(self.installed_apps) != before:
            self.save()
            return True
        return False

    def save(self):
        with open(self.apps_file, "w") as f:
            json.dump(self.installed_apps, f, indent=2)

    def load(self):
        if os.path.exists(self.apps_file):
            with open(self.apps_file, "r") as f:
                self.installed_apps = json.load(f)
        else:
            self.installed_apps = []


class TestApplicationManager(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.mgr = ApplicationManagerMock(self.temp_dir)

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def test_app_registration_and_persistence(self):
        app = self.mgr.register(
            app_id="app-1",
            title="Half-Life 2",
            exe_path="/storage/Games/hl2.exe",
            container_id="container-1",
            graphics="TURNIP_ADRENO",
            resolution="1280x720"
        )
        self.assertEqual(len(self.mgr.installed_apps), 1)
        self.assertEqual(app["title"], "Half-Life 2")
        self.assertEqual(app["graphicsBackend"], "TURNIP_ADRENO")

        # Reload manager from disk to verify JSON persistence
        reloaded = ApplicationManagerMock(self.temp_dir)
        self.assertEqual(len(reloaded.installed_apps), 1)
        self.assertEqual(reloaded.installed_apps[0]["title"], "Half-Life 2")
        self.assertEqual(reloaded.installed_apps[0]["exePath"], "/storage/Games/hl2.exe")

    def test_playtime_tracking(self):
        self.mgr.register("app-2", "Portal", "/storage/Games/portal.exe", "c1")
        self.mgr.record_playtime("app-2", 3600) # 1 hour
        self.assertEqual(self.mgr.installed_apps[0]["totalPlayTimeSeconds"], 3600)
        self.assertEqual(self.mgr.installed_apps[0]["lastPlayedTimestamp"], 1726500000000)

        # Accumulate additional playtime
        self.mgr.record_playtime("app-2", 1800) # +30 minutes
        self.assertEqual(self.mgr.installed_apps[0]["totalPlayTimeSeconds"], 5400)

    def test_app_deletion(self):
        self.mgr.register("app-3", "Skyrim", "/storage/Games/skyrim.exe", "c1")
        self.assertTrue(self.mgr.remove("app-3"))
        self.assertEqual(len(self.mgr.installed_apps), 0)

        # Ensure persistence reflects deletion
        reloaded = ApplicationManagerMock(self.temp_dir)
        self.assertEqual(len(reloaded.installed_apps), 0)

if __name__ == "__main__":
    unittest.main()
