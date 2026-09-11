#!/usr/bin/env python3
"""
Unit and integration tests for WinDroid PE Scanner.
Constructs valid minimal synthetic PE32 (x86) and PE32+ (x86_64) binaries
with section tables and import descriptors to verify architecture and graphics detection.
"""

import os
import struct
import tempfile
import unittest
from pe_scanner import PeScanner

def create_synthetic_pe(
    is_64bit: bool = True,
    machine: int = 0x8664, # AMD64
    subsystem: int = 2,    # Windows GUI
    imported_dlls: list = None
) -> bytes:
    if imported_dlls is None:
        imported_dlls = ["d3d11.dll", "kernel32.dll"]

    buf = bytearray(4096)

    # 1. DOS Header
    struct.pack_into("<H", buf, 0, 0x5A4D) # 'MZ'
    e_lfanew = 128
    struct.pack_into("<I", buf, 0x3C, e_lfanew)

    # 2. PE Signature
    struct.pack_into("<I", buf, e_lfanew, 0x00004550) # 'PE\0\0'

    # 3. COFF Header
    coff_offset = e_lfanew + 4
    num_sections = 2
    opt_hdr_size = 240 if is_64bit else 224
    characteristics = 0x0022 # Executable image, large address aware
    struct.pack_into("<HHIIIHH", buf, coff_offset, machine, num_sections, 0x66000000, 0, 0, opt_hdr_size, characteristics)

    # 4. Optional Header
    opt_offset = coff_offset + 20
    magic = 0x20B if is_64bit else 0x10B
    struct.pack_into("<H", buf, opt_offset, magic)
    struct.pack_into("<I", buf, opt_offset + 16, 0x1000) # EntryPoint RVA

    if is_64bit:
        struct.pack_into("<Q", buf, opt_offset + 24, 0x140000000) # ImageBase
        struct.pack_into("<H", buf, opt_offset + 68, subsystem)
        struct.pack_into("<I", buf, opt_offset + 108, 16) # Num RVAs
        data_dir_offset = opt_offset + 112
    else:
        struct.pack_into("<I", buf, opt_offset + 28, 0x400000) # ImageBase
        struct.pack_into("<H", buf, opt_offset + 68, subsystem)
        struct.pack_into("<I", buf, opt_offset + 92, 16) # Num RVAs
        data_dir_offset = opt_offset + 96

    # Data directory [1] = Import Table
    import_rva = 0x2000
    import_size = 20 * (len(imported_dlls) + 1)
    struct.pack_into("<II", buf, data_dir_offset + 8, import_rva, import_size)

    # 5. Sections
    # .text section (RVA 0x1000, raw 0x400)
    # .rdata section (RVA 0x2000, raw 0x800)
    sec_offset = opt_offset + opt_hdr_size

    # Section 1: .text
    buf[sec_offset:sec_offset+5] = b".text"
    struct.pack_into("<IIIIIIHHI", buf, sec_offset + 8, 0x1000, 0x1000, 0x200, 0x400, 0, 0, 0, 0, 0x60000020)

    # Section 2: .rdata (holds imports)
    sec2_offset = sec_offset + 40
    buf[sec2_offset:sec2_offset+6] = b".rdata"
    struct.pack_into("<IIIIIIHHI", buf, sec2_offset + 8, 0x1000, 0x2000, 0x600, 0x800, 0, 0, 0, 0, 0x40000040)

    # Fill Import Table at raw offset 0x800 (maps to RVA 0x2000)
    raw_import_offset = 0x800
    string_table_rva = 0x2100
    string_table_raw = 0x900

    str_cursor_raw = string_table_raw
    str_cursor_rva = string_table_rva

    for i, dll in enumerate(imported_dlls):
        desc = raw_import_offset + (i * 20)
        dll_bytes = dll.encode('ascii') + b'\x00'
        buf[str_cursor_raw:str_cursor_raw + len(dll_bytes)] = dll_bytes

        struct.pack_into("<IIIII", buf, desc,
                         0x2050,           # OriginalFirstThunk
                         0,                # TimeDate
                         0,                # ForwarderChain
                         str_cursor_rva,   # Name RVA
                         0x2060            # FirstThunk
        )
        str_cursor_raw += len(dll_bytes)
        str_cursor_rva += len(dll_bytes)

    return bytes(buf)


class TestPeScanner(unittest.TestCase):

    def test_64bit_directx11_binary(self):
        data = create_synthetic_pe(
            is_64bit=True,
            machine=0x8664,
            subsystem=2,
            imported_dlls=["d3d11.dll", "kernel32.dll"]
        )
        with tempfile.NamedTemporaryFile(suffix=".exe", delete=False) as f:
            f.write(data)
            temp_path = f.name

        try:
            scanner = PeScanner(temp_path)
            res = scanner.scan()
            self.assertTrue(res["valid"])
            self.assertEqual(res["bitness"], 64)
            self.assertEqual(res["arch_type"], "x86_64")
            self.assertTrue(res["requires_box64"])
            self.assertFalse(res["requires_box86"])
            self.assertEqual(res["recommended_wine_arch"], "win64")
            self.assertEqual(res["detected_graphics"], "DirectX 10/11")
            self.assertEqual(res["recommended_translator"], "DXVK (Vulkan)")
            self.assertIn("d3d11.dll", res["imported_dlls"])
            self.assertIn("kernel32.dll", res["imported_dlls"])
        finally:
            os.unlink(temp_path)

    def test_32bit_directx9_binary(self):
        data = create_synthetic_pe(
            is_64bit=False,
            machine=0x014C, # i386
            subsystem=2,
            imported_dlls=["d3d9.dll", "user32.dll"]
        )
        with tempfile.NamedTemporaryFile(suffix=".exe", delete=False) as f:
            f.write(data)
            temp_path = f.name

        try:
            scanner = PeScanner(temp_path)
            res = scanner.scan()
            self.assertTrue(res["valid"])
            self.assertEqual(res["bitness"], 32)
            self.assertEqual(res["arch_type"], "x86")
            self.assertTrue(res["requires_box86"])
            self.assertFalse(res["requires_box64"])
            self.assertEqual(res["recommended_wine_arch"], "win32")
            self.assertEqual(res["detected_graphics"], "DirectX 9")
            self.assertIn("d3d9.dll", res["imported_dlls"])
        finally:
            os.unlink(temp_path)

    def test_native_arm64_binary(self):
        data = create_synthetic_pe(
            is_64bit=True,
            machine=0xAA64, # ARM64
            subsystem=3,    # Console
            imported_dlls=["ntdll.dll"]
        )
        with tempfile.NamedTemporaryFile(suffix=".exe", delete=False) as f:
            f.write(data)
            temp_path = f.name

        try:
            scanner = PeScanner(temp_path)
            res = scanner.scan()
            self.assertTrue(res["valid"])
            self.assertEqual(res["arch_type"], "arm64")
            self.assertFalse(res["requires_box64"])
            self.assertFalse(res["requires_box86"])
            self.assertIn("Console", res["subsystem"])
        finally:
            os.unlink(temp_path)

    def test_invalid_file(self):
        with tempfile.NamedTemporaryFile(suffix=".exe", delete=False) as f:
            f.write(b"NOT_A_VALID_PE_HEADER_DATA_1234567890")
            temp_path = f.name

        try:
            scanner = PeScanner(temp_path)
            res = scanner.scan()
            self.assertFalse(res["valid"])
        finally:
            os.unlink(temp_path)

if __name__ == "__main__":
    unittest.main()
