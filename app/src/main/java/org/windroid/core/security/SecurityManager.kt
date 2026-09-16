package org.windroid.core.security

import org.windroid.core.pe.PeInspectionResult
import java.io.File
import java.security.MessageDigest

/**
 * Security risk assessment for an untrusted executable.
 */
data class SecurityScanResult(
    val isSafeToLaunch: Boolean,
    val requiresExplicitConsent: Boolean,
    val sha256Checksum: String,
    val warnings: List<String>,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Sandboxing and security policy enforcement for WinDroid.
 * Ensures Windows executables cannot execute uncontained, access host Android storage
 * without explicit permission, or run automatically without user confirmation.
 */
class SecurityManager(private val appFilesDir: File) {

    /**
     * Inspects a candidate executable file for known risk factors before allowing launch.
     */
    fun assessExecutableRisk(file: File, peResult: PeInspectionResult?): SecurityScanResult {
        val warnings = mutableListOf<String>()
        var risk = RiskLevel.LOW

        if (!file.exists()) {
            return SecurityScanResult(
                isSafeToLaunch = false,
                requiresExplicitConsent = false,
                sha256Checksum = "",
                warnings = listOf("Binary file does not exist on disk"),
                riskLevel = RiskLevel.HIGH
            )
        }

        val checksum = calculateSha256(file)

        // Rule 1: Unknown origin check
        if (file.parent?.contains("Download", ignoreCase = true) == true) {
            warnings.add("File is located in the Downloads directory. Executing directly downloaded binaries carries inherent risk.")
            risk = RiskLevel.MEDIUM
        }

        // Rule 2: Non-PE or corrupt PE binary
        if (peResult != null && !peResult.isValidPe) {
            warnings.add("File failed PE header validation: ${peResult.errorMessage ?: "Malformed or corrupted binary"}.")
            risk = RiskLevel.HIGH
        }

        // Rule 3: Kernel or native NT device driver
        if (peResult?.subsystem?.contains("Driver", ignoreCase = true) == true) {
            warnings.add("Executable targets NT kernel-mode driver subsystem. Drivers cannot execute in userspace Wine.")
            risk = RiskLevel.HIGH
        }

        // Rule 4: System DLL tampering
        val protectedNames = setOf("ntdll.dll", "kernel32.dll", "user32.dll", "gdi32.dll", "wineboot.exe")
        if (protectedNames.contains(file.name.lowercase())) {
            warnings.add("Binary matches protected core Wine system component name. Direct execution blocked.")
            risk = RiskLevel.HIGH
        }

        val requiresConsent = risk != RiskLevel.LOW || !isKnownWhitelisted(checksum)

        return SecurityScanResult(
            isSafeToLaunch = risk != RiskLevel.HIGH,
            requiresExplicitConsent = requiresConsent,
            sha256Checksum = checksum,
            warnings = warnings,
            riskLevel = risk
        )
    }

    /**
     * Validates that the requested target path is within an authorized container or mapped drive.
     */
    fun validatePathIsolation(path: String, allowedPrefixDir: File, mappedDrives: List<String>): Boolean {
        val canonicalTarget = try {
            File(path).canonicalPath
        } catch (_: Exception) {
            return false
        }

        val allowedPrefix = allowedPrefixDir.canonicalPath
        if (canonicalTarget.startsWith(allowedPrefix)) {
            return true
        }

        for (drive in mappedDrives) {
            val canonicalDrive = try {
                File(drive).canonicalPath
            } catch (_: Exception) {
                continue
            }
            if (canonicalTarget.startsWith(canonicalDrive)) {
                return true
            }
        }

        return false
    }

    private fun calculateSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "UNKNOWN"
        }
    }

    private fun isKnownWhitelisted(sha256: String): Boolean {
        // Whitelist for common built-in wine utilities
        return false
    }
}
