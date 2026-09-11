package org.windroid.core.container

import java.io.File

/**
 * Manages container lifecycle, prefix initialization, drive mapping,
 * and sandbox isolation on the Android filesystem.
 */
class ContainerManager(private val baseFilesDir: File) {

    val containersDir: File = File(baseFilesDir, "containers").apply { mkdirs() }
    val defaultStorageDir: File = File(baseFilesDir, "storage").apply { mkdirs() }

    fun getContainerDir(containerId: String): File {
        return File(containersDir, containerId).apply { mkdirs() }
    }

    fun getWinePrefixDir(containerId: String): File {
        return File(getContainerDir(containerId), ".wine")
    }

    fun getDosDevicesDir(containerId: String): File {
        return File(getWinePrefixDir(containerId), "dosdevices")
    }

    /**
     * Creates a new isolated container with default drive mappings and prefix scaffolding.
     */
    fun createContainer(profile: ContainerProfile): ContainerProfile {
        val containerDir = getContainerDir(profile.id)
        val prefixDir = getWinePrefixDir(profile.id)
        val dosdevices = getDosDevicesDir(profile.id)

        containerDir.mkdirs()
        prefixDir.mkdirs()
        dosdevices.mkdirs()

        // Create default drive C structure
        val driveC = File(prefixDir, "drive_c")
        File(driveC, "windows/system32").mkdirs()
        File(driveC, "windows/syswow64").mkdirs()
        File(driveC, "Program Files").mkdirs()
        File(driveC, "Program Files (x86)").mkdirs()
        File(driveC, "users/windroid/Desktop").mkdirs()
        File(driveC, "users/windroid/Downloads").mkdirs()

        // Set up default drive mappings
        val updatedDrives = mutableListOf<DriveMapping>()
        updatedDrives.add(DriveMapping('c', driveC.absolutePath, false, "System Drive (C:)"))

        // Add D: drive mapped to shared or SAF storage
        val userGamesDir = File(defaultStorageDir, "Games").apply { mkdirs() }
        updatedDrives.add(DriveMapping('d', userGamesDir.absolutePath, false, "User Applications (D:)"))

        // Configure symlinks or drive pointers in dosdevices
        setupDriveMappings(profile.id, updatedDrives)

        return profile.copy(driveMappings = updatedDrives)
    }

    /**
     * Links mapped folders to Wine's dosdevices (e.g. c:: -> drive_c, d:: -> /storage/...)
     */
    fun setupDriveMappings(containerId: String, drives: List<DriveMapping>) {
        val dosdevices = getDosDevicesDir(containerId)
        dosdevices.mkdirs()

        for (drive in drives) {
            val letter = drive.driveLetter.lowercaseChar()
            val linkFile = File(dosdevices, "$letter:")
            try {
                if (linkFile.exists() || isSymlink(linkFile)) {
                    linkFile.delete()
                }
                // Under Linux/Android, create symlink
                createSymbolicLink(drive.androidPath, linkFile.absolutePath)
            } catch (e: Exception) {
                // Fallback for filesystems without symlink support
            }
        }
    }

    private fun isSymlink(file: File): Boolean {
        return file.canonicalPath != file.absolutePath
    }

    private fun createSymbolicLink(target: String, link: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("ln", "-s", target, link))
            process.waitFor()
        } catch (ignored: Exception) {}
    }

    /**
     * Purges all data associated with a container, preventing orphan files.
     */
    fun deleteContainer(containerId: String): Boolean {
        val containerDir = File(containersDir, containerId)
        return if (containerDir.exists()) {
            containerDir.deleteRecursively()
        } else true
    }

    /**
     * Lists existing containers found on disk.
     */
    fun listContainers(): List<String> {
        return containersDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }
}
