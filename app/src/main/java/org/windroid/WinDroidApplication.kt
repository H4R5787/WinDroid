package org.windroid

import android.app.Application
import org.windroid.core.container.ContainerManager
import org.windroid.core.container.ContainerProfile
import org.windroid.core.runtime.WineRuntimeEngine
import java.io.File

class WinDroidApplication : Application() {

    lateinit var containerManager: ContainerManager
        private set

    lateinit var runtimeEngine: WineRuntimeEngine
        private set

    lateinit var runtimeManager: org.windroid.core.runtime.RuntimeManager
        private set

    lateinit var applicationManager: org.windroid.core.application.ApplicationManager
        private set

    lateinit var securityManager: org.windroid.core.security.SecurityManager
        private set

    override fun onCreate() {
        super.onCreate()
        containerManager = ContainerManager(filesDir)

        val rootfsDir = File(filesDir, "rootfs")
        runtimeEngine = WineRuntimeEngine(rootfsDir, filesDir)
        runtimeManager = org.windroid.core.runtime.RuntimeManager(rootfsDir, filesDir)
        applicationManager = org.windroid.core.application.ApplicationManager(filesDir)
        securityManager = org.windroid.core.security.SecurityManager(filesDir)

        // Initialize default container if none exists
        if (containerManager.listContainers().isEmpty()) {
            val defaultProfile = ContainerProfile(
                id = "default-x64",
                name = "Primary 64-bit Wine Container",
                wineArch = "win64",
                screenResolution = "1280x720"
            )
            containerManager.createContainer(defaultProfile)
        }
    }
}
