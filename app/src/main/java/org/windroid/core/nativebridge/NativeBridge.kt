package org.windroid.core.nativebridge

import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * JNI bridge interfacing Kotlin with native C++ components for PTY terminal emulation,
 * Vulkan hardware probing, shared memory framebuffer transfer, and high-frequency input.
 */
object NativeBridge {

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("windroid_native")
            isLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            // Fallback for host tests / mock environments
            isLoaded = false
        }
    }

    fun isNativeLoaded(): Boolean = isLoaded

    external fun getCpuArchitecture(): String
    external fun checkVulkanSupport(): Int
    external fun detectGpuRenderer(): String
    external fun createPtySession(cmdArgs: Array<String>, envVars: Array<String>, workDir: String): Int
    external fun sendInputEvent(eventType: Int, code: Int, value: Int): Int
    external fun createSharedBuffer(name: String, size: Long): Int
    external fun closePtySession(ptyFd: Int): Int

    // Safe fallback wrappers
    fun safeGetCpuArchitecture(): String {
        return if (isLoaded) {
            try {
                getCpuArchitecture()
            } catch (_: Exception) {
                System.getProperty("os.arch") ?: "aarch64"
            }
        } else {
            System.getProperty("os.arch") ?: "aarch64"
        }
    }

    fun safeDetectGpu(): String {
        return if (isLoaded) {
            try {
                detectGpuRenderer()
            } catch (_: Exception) {
                "Adreno (Turnip Vulkan Ready)"
            }
        } else {
            "Adreno (Turnip Vulkan Ready)"
        }
    }
}
