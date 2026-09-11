package org.windroid.core.container

import java.util.UUID

/**
 * Drive mapping between Android host filesystem (or SAF tree) and Wine virtual drive.
 */
data class DriveMapping(
    val driveLetter: Char, // 'C', 'D', 'E', etc.
    val androidPath: String,
    val isReadOnly: Boolean = false,
    val description: String = ""
)

/**
 * Box64 / Box86 performance preset configuration.
 */
enum class Box64Preset(
    val displayName: String,
    val envVars: Map<String, String>
) {
    SAFE(
        displayName = "Safe (High Compatibility)",
        envVars = mapOf(
            "BOX64_DYNAREC" to "1",
            "BOX64_DYNAREC_FASTNAN" to "0",
            "BOX64_DYNAREC_FASTROUND" to "0",
            "BOX64_DYNAREC_SAFEFLAGS" to "1",
            "BOX64_DYNAREC_CALLRET" to "0"
        )
    ),
    BALANCED(
        displayName = "Balanced (Recommended)",
        envVars = mapOf(
            "BOX64_DYNAREC" to "1",
            "BOX64_DYNAREC_FASTNAN" to "1",
            "BOX64_DYNAREC_FASTROUND" to "0",
            "BOX64_DYNAREC_BIGBLOCK" to "1",
            "BOX64_DYNAREC_SAFEFLAGS" to "1",
            "BOX64_DYNAREC_CALLRET" to "1"
        )
    ),
    AGGRESSIVE(
        displayName = "Aggressive Performance (Max FPS)",
        envVars = mapOf(
            "BOX64_DYNAREC" to "1",
            "BOX64_DYNAREC_FASTNAN" to "1",
            "BOX64_DYNAREC_FASTROUND" to "1",
            "BOX64_DYNAREC_BIGBLOCK" to "2",
            "BOX64_DYNAREC_SAFEFLAGS" to "0",
            "BOX64_DYNAREC_CALLRET" to "1",
            "BOX64_DYNAREC_FORWARD" to "256"
        )
    )
}

/**
 * Isolated container profile representing a distinct Wine prefix and execution context.
 */
data class ContainerProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Container 1",
    val wineArch: String = "win64", // "win32" or "win64"
    val wineVersion: String = "wine-ge-8.26",
    val screenResolution: String = "1280x720",
    val colorDepth: Int = 32,
    val graphicsDriver: String = "Turnip (Adreno Vulkan)",
    val dxvkVersion: String = "dxvk-2.3",
    val vkd3dVersion: String = "vkd3d-proton-2.11",
    val box64Preset: Box64Preset = Box64Preset.BALANCED,
    val showFpsCounter: Boolean = false,
    val isolateNetwork: Boolean = false,
    val audioEnabled: Boolean = true,
    val driveMappings: List<DriveMapping> = emptyList(),
    val customEnvVars: Map<String, String> = emptyMap(),
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
