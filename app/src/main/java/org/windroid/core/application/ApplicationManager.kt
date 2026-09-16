package org.windroid.core.application

import org.windroid.core.container.Box64Preset
import org.windroid.core.container.ContainerProfile
import org.windroid.core.graphics.GraphicsDriverType
import org.windroid.core.pe.PeInspectionResult
import java.io.File
import java.io.Serializable
import java.util.UUID

/**
 * Metadata and configuration for an installed Windows application or game.
 */
data class InstalledApp(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val exePath: String, // Path inside container or Android SAF drive
    val arguments: String = "",
    val containerId: String,
    val iconUri: String? = null,
    val architecture: String = "x86_64", // "x86", "x86_64", "arm64"
    val graphicsBackend: GraphicsDriverType = GraphicsDriverType.TURNIP_ADRENO,
    val resolution: String = "1280x720",
    val box64Preset: Box64Preset = Box64Preset.BALANCED,
    val dxvkEnabled: Boolean = true,
    val vkd3dEnabled: Boolean = false,
    val showFpsCounter: Boolean = false,
    val customEnvVars: Map<String, String> = emptyMap(),
    val installTimestamp: Long = System.currentTimeMillis(),
    val lastPlayedTimestamp: Long = 0L,
    val totalPlayTimeSeconds: Long = 0L,
    val category: String = "Games" // "Games", "Productivity", "Utilities", "Media"
) : Serializable

/**
 * Manages installed Windows applications, shortcuts, playtime tracking,
 * and individual per-application configuration overrides.
 */
class ApplicationManager(private val appDataDir: File) {

    private val appsFile = File(appDataDir, "installed_apps.json")
    private val installedApps = mutableListOf<InstalledApp>()

    init {
        loadApplications()
    }

    @Synchronized
    fun getInstalledApps(): List<InstalledApp> {
        return installedApps.toList()
    }

    @Synchronized
    fun getAppById(id: String): InstalledApp? {
        return installedApps.find { it.id == id }
    }

    @Synchronized
    fun registerApplication(
        title: String,
        exePath: String,
        container: ContainerProfile,
        peInfo: PeInspectionResult? = null,
        arguments: String = "",
        category: String = "Games"
    ): InstalledApp {
        val app = InstalledApp(
            title = title,
            exePath = exePath,
            arguments = arguments,
            containerId = container.id,
            architecture = peInfo?.targetArchitecture ?: container.wineArch,
            resolution = container.screenResolution,
            box64Preset = container.box64Preset,
            dxvkEnabled = peInfo?.isDirectX ?: true,
            vkd3dEnabled = peInfo?.importedDirectXVersion == "DirectX 12",
            showFpsCounter = container.showFpsCounter,
            category = category
        )
        installedApps.removeAll { it.id == app.id || (it.exePath == exePath && it.containerId == container.id) }
        installedApps.add(0, app)
        saveApplications()
        return app
    }

    @Synchronized
    fun updateApplication(app: InstalledApp) {
        val index = installedApps.indexOfFirst { it.id == app.id }
        if (index != -1) {
            installedApps[index] = app
            saveApplications()
        }
    }

    @Synchronized
    fun removeApplication(appId: String): Boolean {
        val removed = installedApps.removeAll { it.id == appId }
        if (removed) {
            saveApplications()
        }
        return removed
    }

    @Synchronized
    fun recordSessionPlayTime(appId: String, elapsedSeconds: Long) {
        val index = installedApps.indexOfFirst { it.id == appId }
        if (index != -1) {
            val current = installedApps[index]
            installedApps[index] = current.copy(
                lastPlayedTimestamp = System.currentTimeMillis(),
                totalPlayTimeSeconds = current.totalPlayTimeSeconds + elapsedSeconds
            )
            saveApplications()
        }
    }

    private fun loadApplications() {
        if (!appsFile.exists()) {
            return
        }
        try {
            val content = appsFile.readText()
            val items = parseAppsJson(content)
            installedApps.clear()
            installedApps.addAll(items)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveApplications() {
        try {
            appsFile.parentFile?.mkdirs()
            val json = serializeAppsToJson(installedApps)
            appsFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeAppsToJson(apps: List<InstalledApp>): String {
        val sb = StringBuilder("[\n")
        apps.forEachIndexed { index, app ->
            sb.append("  {\n")
            sb.append("    \"id\": \"${app.id}\",\n")
            sb.append("    \"title\": \"${escapeJson(app.title)}\",\n")
            sb.append("    \"exePath\": \"${escapeJson(app.exePath)}\",\n")
            sb.append("    \"arguments\": \"${escapeJson(app.arguments)}\",\n")
            sb.append("    \"containerId\": \"${app.containerId}\",\n")
            sb.append("    \"architecture\": \"${app.architecture}\",\n")
            sb.append("    \"graphicsBackend\": \"${app.graphicsBackend.name}\",\n")
            sb.append("    \"resolution\": \"${app.resolution}\",\n")
            sb.append("    \"box64Preset\": \"${app.box64Preset.name}\",\n")
            sb.append("    \"dxvkEnabled\": ${app.dxvkEnabled},\n")
            sb.append("    \"vkd3dEnabled\": ${app.vkd3dEnabled},\n")
            sb.append("    \"showFpsCounter\": ${app.showFpsCounter},\n")
            sb.append("    \"installTimestamp\": ${app.installTimestamp},\n")
            sb.append("    \"lastPlayedTimestamp\": ${app.lastPlayedTimestamp},\n")
            sb.append("    \"totalPlayTimeSeconds\": ${app.totalPlayTimeSeconds},\n")
            sb.append("    \"category\": \"${escapeJson(app.category)}\"\n")
            sb.append("  }${if (index < apps.size - 1) "," else ""}\n")
        }
        sb.append("]\n")
        return sb.toString()
    }

    private fun parseAppsJson(json: String): List<InstalledApp> {
        val result = mutableListOf<InstalledApp>()
        val blocks = json.split("},\\s*\\{".toRegex())
        for (b in blocks) {
            val title = extractJsonField(b, "title") ?: continue
            val exePath = extractJsonField(b, "exePath") ?: continue
            val id = extractJsonField(b, "id") ?: UUID.randomUUID().toString()
            val containerId = extractJsonField(b, "containerId") ?: "default"
            val args = extractJsonField(b, "arguments") ?: ""
            val arch = extractJsonField(b, "architecture") ?: "x86_64"
            val res = extractJsonField(b, "resolution") ?: "1280x720"
            val category = extractJsonField(b, "category") ?: "Games"
            val totalPlayTime = extractJsonField(b, "totalPlayTimeSeconds")?.toLongOrNull() ?: 0L
            val lastPlayed = extractJsonField(b, "lastPlayedTimestamp")?.toLongOrNull() ?: 0L
            val installTime = extractJsonField(b, "installTimestamp")?.toLongOrNull() ?: System.currentTimeMillis()
            val presetName = extractJsonField(b, "box64Preset") ?: "BALANCED"
            val preset = try { Box64Preset.valueOf(presetName) } catch (_: Exception) { Box64Preset.BALANCED }
            val backendName = extractJsonField(b, "graphicsBackend") ?: "TURNIP_ADRENO"
            val backend = try { GraphicsDriverType.valueOf(backendName) } catch (_: Exception) { GraphicsDriverType.TURNIP_ADRENO }

            result.add(
                InstalledApp(
                    id = id,
                    title = title,
                    exePath = exePath,
                    arguments = args,
                    containerId = containerId,
                    architecture = arch,
                    graphicsBackend = backend,
                    resolution = res,
                    box64Preset = preset,
                    totalPlayTimeSeconds = totalPlayTime,
                    lastPlayedTimestamp = lastPlayed,
                    installTimestamp = installTime,
                    category = category
                )
            )
        }
        return result
    }

    private fun extractJsonField(block: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"?([^\",\\n\\}]+)\"?".toRegex()
        return pattern.find(block)?.groupValues?.get(1)?.trim()
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }
}
