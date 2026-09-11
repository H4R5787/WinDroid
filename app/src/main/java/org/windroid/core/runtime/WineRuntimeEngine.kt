package org.windroid.core.runtime

import org.windroid.core.container.ContainerProfile
import org.windroid.core.pe.PeInspectionResult
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Log entry emitted by Wine, Box64, or the runtime environment.
 */
data class RuntimeLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val source: String, // "WINE", "BOX64", "DXVK", "SYSTEM"
    val message: String
)

/**
 * Execution and process orchestration engine for launching Windows binaries
 * under PRoot/Linux rootfs with Box64 dynamic binary translation and Wine.
 */
class WineRuntimeEngine(
    private val rootfsDir: File,
    private val appFilesDir: File
) {

    private var activeProcess: Process? = null
    private val logListeners = CopyOnWriteArrayList<(RuntimeLogEntry) -> Unit>()
    private val logHistory = CopyOnWriteArrayList<RuntimeLogEntry>()

    fun addLogListener(listener: (RuntimeLogEntry) -> Unit) {
        logListeners.add(listener)
    }

    fun removeLogListener(listener: (RuntimeLogEntry) -> Unit) {
        logListeners.remove(listener)
    }

    fun getLogs(): List<RuntimeLogEntry> = logHistory.toList()

    private fun emitLog(source: String, message: String) {
        val entry = RuntimeLogEntry(source = source, message = message)
        logHistory.add(entry)
        if (logHistory.size > 2000) {
            logHistory.removeAt(0)
        }
        logListeners.forEach { it(entry) }
    }

    /**
     * Builds the complete environment variable map for Box64, Wine, DXVK, and X11.
     */
    fun buildEnvironment(
        profile: ContainerProfile,
        peInfo: PeInspectionResult?
    ): Map<String, String> {
        val env = mutableMapOf<String, String>()

        // Wine Environment
        val prefix = File(File(appFilesDir, "containers"), "${profile.id}/.wine").absolutePath
        env["WINEPREFIX"] = prefix
        env["WINEARCH"] = profile.wineArch
        env["WINEDEBUG"] = "-all"
        env["WINEESYNC"] = "0" // Fsync/Esync requires kernel support
        env["WINEDLLOVERRIDES"] = "mscoree,mshtml="

        // Display & Graphics
        env["DISPLAY"] = ":0"
        env["PULSE_SERVER"] = "127.0.0.1:4713"
        env["GALLIUM_DRIVER"] = "zink"
        env["MESA_LOADER_DRIVER_OVERRIDE"] = "zink"

        // DXVK Configuration
        if (profile.showFpsCounter) {
            env["DXVK_HUD"] = "fps,devinfo"
        }
        env["DXVK_STATE_CACHE"] = "1"
        env["DXVK_LOG_LEVEL"] = "info"

        // Box64 / Box86 Dynarec Tuning
        env.putAll(profile.box64Preset.envVars)
        env["BOX64_LOG"] = "1"
        env["BOX64_NOBANNER"] = "1"
        env["BOX64_AVX"] = "1"

        // Custom Overrides
        env.putAll(profile.customEnvVars)

        return env
    }

    /**
     * Formats the execution command for PRoot + Box64 + Wine + Target Executable.
     */
    fun buildCommandLine(
        targetExe: File,
        exeArgs: List<String>,
        profile: ContainerProfile
    ): List<String> {
        val cmd = mutableListOf<String>()

        // 1. PRoot sandbox invocation
        val prootBin = File(rootfsDir, "usr/bin/proot").absolutePath
        cmd.add(prootBin)
        cmd.add("-0")
        cmd.add("-r")
        cmd.add(rootfsDir.absolutePath)
        cmd.add("-b")
        cmd.add("/dev")
        cmd.add("-b")
        cmd.add("/proc")
        cmd.add("-b")
        cmd.add("/sys")
        cmd.add("-b")
        cmd.add(appFilesDir.absolutePath)

        // Bind drive mappings
        for (drive in profile.driveMappings) {
            cmd.add("-b")
            cmd.add(drive.androidPath)
        }

        cmd.add("-w")
        cmd.add(targetExe.parentFile?.absolutePath ?: rootfsDir.absolutePath)

        // 2. Box64 / Wine invocation inside container
        cmd.add("/usr/local/bin/box64")
        cmd.add("/usr/local/bin/wine")
        cmd.add(targetExe.absolutePath)
        cmd.addAll(exeArgs)

        return cmd
    }

    /**
     * Executes the target Windows executable and streams logs asynchronously.
     */
    fun launch(
        targetExe: File,
        exeArgs: List<String> = emptyList(),
        profile: ContainerProfile,
        peInfo: PeInspectionResult? = null
    ): Boolean {
        if (activeProcess?.isAlive == true) {
            emitLog("SYSTEM", "A process is already running. Terminate it first.")
            return false
        }

        val envVars = buildEnvironment(profile, peInfo)
        val cmdLine = buildCommandLine(targetExe, exeArgs, profile)

        emitLog("SYSTEM", "Launching: ${targetExe.name}")
        emitLog("SYSTEM", "Command: ${cmdLine.joinToString(" ")}")

        return try {
            val processBuilder = ProcessBuilder(cmdLine)
            val processEnv = processBuilder.environment()
            processEnv.putAll(envVars)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            activeProcess = process

            // Background thread to read stdout/stderr
            Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val msg = line ?: continue
                            val source = when {
                                msg.contains("wine", ignoreCase = true) -> "WINE"
                                msg.contains("box64", ignoreCase = true) -> "BOX64"
                                msg.contains("dxvk", ignoreCase = true) -> "DXVK"
                                else -> "RUNTIME"
                            }
                            emitLog(source, msg)
                        }
                    }
                } catch (e: Exception) {
                    emitLog("SYSTEM", "Log reader error: ${e.message}")
                } finally {
                    emitLog("SYSTEM", "Process terminated with exit code ${process.exitValue()}")
                }
            }.start()

            true
        } catch (e: Exception) {
            emitLog("SYSTEM", "Failed to launch process: ${e.message}")
            false
        }
    }

    /**
     * Gracefully stops the active process or forces kill if unresponsive.
     */
    fun terminate(): Boolean {
        val proc = activeProcess ?: return false
        emitLog("SYSTEM", "Terminating active process...")
        proc.destroy()
        return true
    }

    fun isRunning(): Boolean = activeProcess?.isAlive == true
}
