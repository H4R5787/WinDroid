package org.windroid.core.pe

/**
 * Architecture type parsed from PE COFF Machine field.
 */
enum class PeArchitecture(val machineCode: Int, val displayName: String, val bitness: Int) {
    I386(0x014c, "Intel 386 / x86 (32-bit)", 32),
    AMD64(0x8664, "AMD64 / x86_64 (64-bit)", 64),
    ARM64(0xaa64, "ARM64 / AArch64 (Native)", 64),
    ARMNT(0x01c4, "ARM Thumb-2", 32),
    IA64(0x0200, "Intel Itanium (IA-64)", 64),
    UNKNOWN(0x0000, "Unknown Machine", 0);

    companion object {
        fun fromMachine(code: Int): PeArchitecture =
            entries.find { it.machineCode == code } ?: UNKNOWN
    }
}

/**
 * Windows PE Subsystem type.
 */
enum class PeSubsystem(val code: Int, val description: String) {
    UNKNOWN(0, "Unknown Subsystem"),
    NATIVE(1, "Device Drivers and Native NT processes"),
    WINDOWS_GUI(2, "Windows Graphical User Interface (GUI)"),
    WINDOWS_CUI(3, "Windows Character/Console Subsystem (CLI)"),
    OS2_CUI(5, "OS/2 Character Subsystem"),
    POSIX_CUI(7, "POSIX Character Subsystem"),
    WINDOWS_CE_GUI(9, "Windows CE GUI"),
    EFI_APPLICATION(10, "EFI Application");

    companion object {
        fun fromCode(code: Int): PeSubsystem =
            entries.find { it.code == code } ?: UNKNOWN
    }
}

/**
 * Detected graphics/rendering subsystem requirements based on imported DLLs.
 */
enum class GraphicsBackendRequirement(val displayName: String, val recommendedTranslator: String) {
    DIRECTX_12("DirectX 12 (D3D12)", "VKD3D-Proton (Vulkan)"),
    DIRECTX_10_11("DirectX 10 / 11 (D3D10/D3D11)", "DXVK (Vulkan)"),
    DIRECTX_9("DirectX 9 (D3D9)", "DXVK (Vulkan) / WineD3D"),
    DIRECTX_8_LEGACY("DirectX 8 or earlier", "D8VK / WineD3D"),
    OPENGL("OpenGL", "Mesa Zink / Native EGL"),
    VULKAN("Vulkan Native", "Vulkan Passthrough ICD"),
    GDI_SOFTWARE("GDI / Software Rendering", "X11 / Wine GDI");
}

/**
 * Section header representation from PE file.
 */
data class PeSection(
    val name: String,
    val virtualSize: Long,
    val virtualAddress: Long,
    val rawDataSize: Long,
    val rawDataPointer: Long,
    val characteristics: Long
)

/**
 * Comprehensive inspection report returned by PeParser.
 */
data class PeInspectionResult(
    val fileName: String,
    val fileSize: Long,
    val isValidPe: Boolean,
    val errorMessage: String? = null,
    val architecture: PeArchitecture = PeArchitecture.UNKNOWN,
    val is64Bit: Boolean = false,
    val subsystem: PeSubsystem = PeSubsystem.UNKNOWN,
    val entryPointRva: Long = 0L,
    val imageBase: Long = 0L,
    val numberOfSections: Int = 0,
    val sections: List<PeSection> = emptyList(),
    val importedDlls: List<String> = emptyList(),
    val detectedGraphics: GraphicsBackendRequirement = GraphicsBackendRequirement.GDI_SOFTWARE,
    val requiresBox86: Boolean = false,
    val requiresBox64: Boolean = false,
    val isDotNetAssembly: Boolean = false,
    val recommendedBox64Preset: String = "Balanced",
    val recommendedWinePrefixArch: String = "win64"
)
