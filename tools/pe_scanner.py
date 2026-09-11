#!/usr/bin/env python3
"""
WinDroid PE Header & Compatibility Scanner CLI.
Analyzes Windows Portable Executable (PE32 / PE32+) binaries to determine:
- Target architecture (i386 32-bit vs AMD64 64-bit vs ARM64)
- Subsystem (GUI vs CUI)
- Graphics requirements (DirectX 9/11/12, Vulkan, OpenGL)
- Recommended Box64 dynarec preset & Wine prefix architecture.
"""

import sys
import os
import struct
from typing import List, Dict, Optional, Tuple

MACHINE_MAP = {
    0x014C: ("Intel 386 / x86 (32-bit)", 32, "x86", "win32"),
    0x8664: ("AMD64 / x86_64 (64-bit)", 64, "x86_64", "win64"),
    0xAA64: ("ARM64 / AArch64 (Native)", 64, "arm64", "win64"),
    0x01C4: ("ARM Thumb-2", 32, "arm", "win32"),
    0x0200: ("Intel Itanium (IA-64)", 64, "ia64", "win64"),
}

SUBSYSTEM_MAP = {
    1: "Device Drivers and Native NT processes",
    2: "Windows Graphical User Interface (GUI)",
    3: "Windows Character / Console Subsystem (CUI)",
    5: "OS/2 Character Subsystem",
    7: "POSIX Character Subsystem",
    9: "Windows CE GUI",
    10: "EFI Application"
}

class PeScanner:
    def __init__(self, filepath: str):
        self.filepath = filepath
        self.filesize = os.path.getsize(filepath) if os.path.exists(filepath) else 0

    def scan(self) -> Dict:
        if not os.path.exists(self.filepath):
            return {"valid": False, "error": f"File not found: {self.filepath}"}

        with open(self.filepath, "rb") as f:
            header_data = f.read(min(self.filesize, 8 * 1024 * 1024))

        if len(header_data) < 64:
            return {"valid": False, "error": "File smaller than DOS header (64 bytes)"}

        # 1. DOS Header
        dos_magic = struct.unpack_from("<H", header_data, 0)[0]
        if dos_magic != 0x5A4D: # 'MZ'
            return {"valid": False, "error": "Invalid DOS header magic (not 'MZ')"}

        e_lfanew = struct.unpack_from("<I", header_data, 0x3C)[0]
        if e_lfanew + 24 > len(header_data):
            return {"valid": False, "error": f"Invalid e_lfanew offset: {e_lfanew}"}

        # 2. PE Signature
        pe_sig = struct.unpack_from("<I", header_data, e_lfanew)[0]
        if pe_sig != 0x00004550: # 'PE\0\0'
            return {"valid": False, "error": "Invalid PE signature (not 'PE\\0\\0')"}

        # 3. COFF Header (20 bytes)
        coff_offset = e_lfanew + 4
        machine, num_sections, timestamp, sym_ptr, num_syms, opt_hdr_size, characteristics = struct.unpack_from(
            "<HHIIIHH", header_data, coff_offset
        )

        mach_info = MACHINE_MAP.get(machine, ("Unknown Machine (0x%04X)" % machine, 0, "unknown", "win64"))

        # 4. Optional Header
        opt_offset = coff_offset + 20
        if opt_hdr_size < 2 or opt_offset + opt_hdr_size > len(header_data):
            return {
                "valid": True,
                "architecture": mach_info[0],
                "bitness": mach_info[1],
                "num_sections": num_sections,
                "sections": [],
                "imports": []
            }

        opt_magic = struct.unpack_from("<H", header_data, opt_offset)[0]
        is_64bit = (opt_magic == 0x20B) # PE32+

        entry_point = struct.unpack_from("<I", header_data, opt_offset + 16)[0]
        if is_64bit:
            image_base = struct.unpack_from("<Q", header_data, opt_offset + 24)[0]
            subsystem = struct.unpack_from("<H", header_data, opt_offset + 68)[0]
            num_rva_sizes = struct.unpack_from("<I", header_data, opt_offset + 108)[0]
            data_dir_offset = opt_offset + 112
        else:
            image_base = struct.unpack_from("<I", header_data, opt_offset + 28)[0]
            subsystem = struct.unpack_from("<H", header_data, opt_offset + 68)[0]
            num_rva_sizes = struct.unpack_from("<I", header_data, opt_offset + 92)[0]
            data_dir_offset = opt_offset + 96

        import_rva, import_size = 0, 0
        if num_rva_sizes >= 2 and data_dir_offset + 16 <= len(header_data):
            import_rva, import_size = struct.unpack_from("<II", header_data, data_dir_offset + 8)

        # 5. Section Table
        sec_table_offset = opt_offset + opt_hdr_size
        sections = []
        for i in range(num_sections):
            sec_offset = sec_table_offset + (i * 40)
            if sec_offset + 40 > len(header_data):
                break
            sec_name_raw = header_data[sec_offset:sec_offset + 8].split(b'\x00')[0]
            sec_name = sec_name_raw.decode('utf-8', errors='ignore')
            v_size, v_addr, raw_size, raw_ptr, _, _, _, _, sec_flags = struct.unpack_from(
                "<IIIIIIHHI", header_data, sec_offset + 8
            )
            sections.append({
                "name": sec_name,
                "virtual_address": v_addr,
                "virtual_size": v_size,
                "raw_size": raw_size,
                "raw_pointer": raw_ptr,
                "flags": sec_flags
            })

        def rva_to_offset(rva: int) -> Optional[int]:
            for s in sections:
                if s["virtual_address"] <= rva < s["virtual_address"] + s["virtual_size"]:
                    return s["raw_pointer"] + (rva - s["virtual_address"])
            return None

        # 6. Import Directory Table
        imported_dlls = []
        if import_rva > 0 and import_size > 0:
            import_file_offset = rva_to_offset(import_rva)
            if import_file_offset is not None and import_file_offset < len(header_data):
                cur = import_file_offset
                while cur + 20 <= len(header_data):
                    orig_thunk, timedate, fwd_chain, name_rva, first_thunk = struct.unpack_from(
                        "<IIIII", header_data, cur
                    )
                    if orig_thunk == 0 and first_thunk == 0 and name_rva == 0:
                        break # End of table
                    dll_name_offset = rva_to_offset(name_rva)
                    if dll_name_offset is not None and dll_name_offset < len(header_data):
                        dll_name_bytes = bytearray()
                        p = dll_name_offset
                        while p < len(header_data) and header_data[p] != 0:
                            dll_name_bytes.append(header_data[p])
                            p += 1
                        dll_name = dll_name_bytes.decode('ascii', errors='ignore').strip()
                        if dll_name and dll_name not in imported_dlls:
                            imported_dlls.append(dll_name)
                    cur += 20

        # Graphics & Subsystem analysis
        graphics_type, translator = self._detect_graphics(imported_dlls)
        requires_box64 = (mach_info[2] == "x86_64")
        requires_box86 = (mach_info[2] == "x86")

        return {
            "valid": True,
            "filename": os.path.basename(self.filepath),
            "filesize_bytes": self.filesize,
            "architecture": mach_info[0],
            "arch_type": mach_info[2],
            "bitness": mach_info[1],
            "recommended_wine_arch": mach_info[3],
            "subsystem": SUBSYSTEM_MAP.get(subsystem, f"Other ({subsystem})"),
            "entry_point": hex(entry_point),
            "image_base": hex(image_base),
            "num_sections": len(sections),
            "sections": sections,
            "imported_dlls": imported_dlls,
            "detected_graphics": graphics_type,
            "recommended_translator": translator,
            "requires_box64": requires_box64,
            "requires_box86": requires_box86,
            "is_dotnet": any(d.lower() == "mscoree.dll" for d in imported_dlls),
            "recommended_box64_preset": "Aggressive Performance" if "DirectX" in graphics_type else "Balanced"
        }

    def _detect_graphics(self, imports: List[str]) -> Tuple[str, str]:
        lower = [x.lower() for x in imports]
        if any(d == "d3d12.dll" for d in lower):
            return "DirectX 12", "VKD3D-Proton (Vulkan)"
        if any(d in ["d3d11.dll", "d3d10.dll", "dxgi.dll"] for d in lower):
            return "DirectX 10/11", "DXVK (Vulkan)"
        if any(d == "d3d9.dll" for d in lower):
            return "DirectX 9", "DXVK (Vulkan) / WineD3D"
        if any(d in ["d3d8.dll", "ddraw.dll"] for d in lower):
            return "DirectX 8 or legacy", "D8VK / WineD3D"
        if any(d == "vulkan-1.dll" for d in lower):
            return "Vulkan Native", "Vulkan Passthrough"
        if any(d == "opengl32.dll" for d in lower):
            return "OpenGL", "Mesa Zink / Native EGL"
        return "GDI / Software", "Wine GDI (X11)"

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: pe_scanner.py <path-to-exe-or-msi>")
        sys.exit(1)

    scanner = PeScanner(sys.argv[1])
    result = scanner.scan()

    if not result.get("valid"):
        print(f"Error: {result.get('error')}")
        sys.exit(1)

    print("==================================================")
    print(" WinDroid PE Binary Compatibility Inspection Report")
    print("==================================================")
    print(f"File:            {result['filename']} ({result['filesize_bytes']} bytes)")
    print(f"Architecture:    {result['architecture']}")
    print(f"Subsystem:       {result['subsystem']}")
    print(f"Bitness:         {result['bitness']}-bit")
    print(f"Wine Prefix:     {result['recommended_wine_arch'].upper()}")
    print(f"Translation:     Box64={result['requires_box64']}, Box86={result['requires_box86']}")
    print(f"Dynarec Preset:  {result['recommended_box64_preset']}")
    print(f"Graphics API:    {result['detected_graphics']}")
    print(f"Translator:      {result['recommended_translator']}")
    print(f"DotNet Assembly: {result['is_dotnet']}")
    print(f"Imported DLLs ({len(result['imported_dlls'])}):")
    for dll in result['imported_dlls']:
        print(f"  - {dll}")
    print("==================================================")
