package org.windroid.core.graphics

/**
 * Graphics driver selection for 3D hardware acceleration on Android devices.
 */
enum class GraphicsDriverType(
    val id: String,
    val displayName: String,
    val icdPath: String?,
    val recommendedGpu: String
) {
    TURNIP_ADRENO(
        id = "turnip",
        displayName = "Mesa Turnip (Adreno Vulkan)",
        icdPath = "/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json",
        recommendedGpu = "Qualcomm Snapdragon 6xx/7xx/8xx"
    ),
    SYSTEM_VULKAN(
        id = "system",
        displayName = "System Native Vulkan (libvulkan.so)",
        icdPath = null,
        recommendedGpu = "Broad compatibility / MediaTek / Exynos"
    ),
    PANFROST(
        id = "panfrost",
        displayName = "Mesa Panfrost (Mali Vulkan)",
        icdPath = "/usr/share/vulkan/icd.d/panfrost_icd.aarch64.json",
        recommendedGpu = "ARM Mali G-series"
    ),
    VIRGL(
        id = "virgl",
        displayName = "VirGL (OpenGL Virtualized)",
        icdPath = null,
        recommendedGpu = "Fallback for non-Vulkan devices"
    ),
    LLVMPIPE_SOFTWARE(
        id = "llvmpipe",
        displayName = "LLVMpipe (CPU Software Rendering)",
        icdPath = null,
        recommendedGpu = "Debug / Software only (Slow)"
    )
}

/**
 * Supported display resolutions for virtual desktop.
 */
enum class DisplayResolution(val width: Int, val height: Int, val label: String) {
    RES_720P(1280, 720, "1280x720 (16:9 - Recommended for Performance)"),
    RES_800P(1280, 800, "1280x800 (16:10)"),
    RES_1080P(1920, 1080, "1920x1080 (Full HD)"),
    RES_480P(854, 480, "854x480 (Low Spec Devices)"),
    RES_NATIVE(0, 0, "Native Android Device Resolution");

    val formattedString: String
        get() = if (width == 0) "Native" else "${width}x${height}"
}
