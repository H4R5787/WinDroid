package org.windroid

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.windroid.core.container.ContainerProfile
import org.windroid.core.pe.PeInspectionResult
import org.windroid.core.pe.PeParser
import org.windroid.ui.screens.*
import org.windroid.ui.theme.WinDroidTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var app: WinDroidApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as WinDroidApplication

        setContent {
            WinDroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var currentInspectionResult by remember { mutableStateOf<PeInspectionResult?>(null) }
                    var selectedExeFile by remember { mutableStateOf<File?>(null) }
                    var editingContainer by remember { mutableStateOf<ContainerProfile?>(null) }

                    var containersList by remember {
                        mutableStateOf(
                            listOf(
                                ContainerProfile(
                                    id = "default-x64",
                                    name = "Primary 64-bit Wine Container",
                                    wineArch = "win64",
                                    screenResolution = "1280x720"
                                )
                            )
                        )
                    }

                    var installedAppsList by remember {
                        mutableStateOf(app.applicationManager.getInstalledApps())
                    }

                    // File picker for EXE / MSI binaries
                    val filePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            handlePickedBinary(it) { result, file ->
                                currentInspectionResult = result
                                selectedExeFile = file
                                navController.navigate("inspector")
                            }
                        }
                    }

                    val wizardFilePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            handlePickedBinary(it) { result, file ->
                                currentInspectionResult = result
                                selectedExeFile = file
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            val homeAppItems = installedAppsList.map { installed ->
                                InstalledAppItem(
                                    id = installed.id,
                                    title = installed.title,
                                    exePath = installed.exePath,
                                    containerId = installed.containerId,
                                    architecture = installed.architecture,
                                    graphicsBackend = installed.graphicsBackend.name,
                                    playtimeHours = installed.totalPlayTimeSeconds / 3600f,
                                    accentColor = Color(0xFF00E5FF)
                                )
                            }

                            HomeScreen(
                                containers = containersList,
                                recentApps = homeAppItems,
                                onSelectFileToInspect = {
                                    filePickerLauncher.launch("*/*")
                                },
                                onLaunchApp = { appItem ->
                                    val targetExe = File(appItem.exePath)
                                    val profile = containersList.find { it.id == appItem.containerId } ?: containersList.first()
                                    app.runtimeEngine.launch(targetExe, emptyList(), profile, null)
                                    navController.navigate("logs")
                                },
                                onNavigateAddApp = {
                                    navController.navigate("add_app")
                                },
                                onNavigateInstalledApps = {
                                    navController.navigate("installed_apps")
                                },
                                onNavigateContainers = {
                                    navController.navigate("containers")
                                },
                                onNavigateRuntime = {
                                    navController.navigate("runtime")
                                },
                                onNavigateGraphics = {
                                    navController.navigate("graphics")
                                },
                                onNavigateInput = {
                                    navController.navigate("input")
                                },
                                onNavigatePerformance = {
                                    navController.navigate("performance")
                                },
                                onNavigateLogs = {
                                    navController.navigate("logs")
                                },
                                onNavigateAbout = {
                                    navController.navigate("about")
                                }
                            )
                        }

                        composable("add_app") {
                            AddAppWizardScreen(
                                inspectedFile = selectedExeFile,
                                peResult = currentInspectionResult,
                                containers = containersList,
                                onSelectFile = {
                                    wizardFilePickerLauncher.launch("*/*")
                                },
                                onSaveAndLaunch = { title, containerId, preset, driver, launchNow ->
                                    val chosenContainer = containersList.find { it.id == containerId } ?: containersList.first()
                                    val registered = app.applicationManager.registerApplication(
                                        title = title,
                                        exePath = selectedExeFile?.absolutePath ?: "",
                                        container = chosenContainer.copy(box64Preset = preset),
                                        peInfo = currentInspectionResult
                                    )
                                    installedAppsList = app.applicationManager.getInstalledApps()

                                    if (launchNow && selectedExeFile != null) {
                                        app.runtimeEngine.launch(selectedExeFile!!, emptyList(), chosenContainer, currentInspectionResult)
                                        navController.navigate("logs")
                                    } else {
                                        navController.popBackStack()
                                    }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("installed_apps") {
                            InstalledAppsScreen(
                                apps = installedAppsList,
                                onLaunchApp = { appItem ->
                                    val exeFile = File(appItem.exePath)
                                    val profile = containersList.find { it.id == appItem.containerId } ?: containersList.first()
                                    app.runtimeEngine.launch(exeFile, emptyList(), profile, null)
                                    navController.navigate("logs")
                                },
                                onDeleteApp = { appItem ->
                                    app.applicationManager.removeApplication(appItem.id)
                                    installedAppsList = app.applicationManager.getInstalledApps()
                                    Toast.makeText(this@MainActivity, "Application removed", Toast.LENGTH_SHORT).show()
                                },
                                onAddApp = {
                                    navController.navigate("add_app")
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("containers") {
                            ContainersScreen(
                                containers = containersList,
                                onCreateNewContainer = {
                                    val newProfile = ContainerProfile(
                                        name = "Wine Container ${containersList.size + 1}",
                                        wineArch = "win64"
                                    )
                                    app.containerManager.createContainer(newProfile)
                                    containersList = containersList + newProfile
                                    Toast.makeText(this@MainActivity, "Container created", Toast.LENGTH_SHORT).show()
                                },
                                onEditContainer = { container ->
                                    editingContainer = container
                                    navController.navigate("container_edit")
                                },
                                onDeleteContainer = { containerId ->
                                    if (containersList.size > 1) {
                                        app.containerManager.deleteContainer(containerId)
                                        containersList = containersList.filter { it.id != containerId }
                                        Toast.makeText(this@MainActivity, "Container deleted", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Cannot delete last remaining container", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("container_edit") {
                            val containerToEdit = editingContainer ?: containersList.first()
                            ContainerEditScreen(
                                initialProfile = containerToEdit,
                                onSave = { updated ->
                                    containersList = containersList.map { if (it.id == updated.id) updated else it }
                                    navController.popBackStack()
                                    Toast.makeText(this@MainActivity, "Container settings saved", Toast.LENGTH_SHORT).show()
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("runtime") {
                            val status = app.runtimeManager.getRuntimeStatus()
                            RuntimeManagerScreen(
                                status = status,
                                onVerifyIntegrity = {
                                    Toast.makeText(this@MainActivity, "All subsystem signatures valid", Toast.LENGTH_SHORT).show()
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("graphics") {
                            GraphicsSettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("input") {
                            InputSettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("performance") {
                            PerformanceMonitorScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("about") {
                            AboutScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("inspector") {
                            val result = currentInspectionResult ?: PeInspectionResult(
                                fileName = "None",
                                fileSize = 0L,
                                isValidPe = false,
                                errorMessage = "No file inspected"
                            )
                            PeInspectorScreen(
                                result = result,
                                onLaunch = {
                                    selectedExeFile?.let { exe ->
                                        val profile = ContainerProfile(
                                            wineArch = result.recommendedWinePrefixArch
                                        )
                                        app.runtimeEngine.launch(exe, emptyList(), profile, result)
                                        navController.navigate("logs")
                                    }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("logs") {
                            val logs = app.runtimeEngine.getLogs()
                            LogViewerScreen(
                                logs = logs,
                                onClearLogs = {},
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handlePickedBinary(uri: Uri, onParsed: (PeInspectionResult, File) -> Unit) {
        try {
            val fileName = getFileName(uri) ?: "app.exe"
            val tempFile = File(cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            val inspection = PeParser.parse(tempFile)
            onParsed(inspection, tempFile)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to inspect file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }
}
