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

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                containers = listOf(
                                    ContainerProfile(name = "Default 64-bit Container", wineArch = "win64")
                                ),
                                recentApps = emptyList(),
                                onSelectFileToInspect = {
                                    filePickerLauncher.launch("*/*")
                                },
                                onLaunchApp = { appItem ->
                                    Toast.makeText(this@MainActivity, "Launching ${appItem.title}", Toast.LENGTH_SHORT).show()
                                },
                                onNavigateContainers = {
                                    navController.navigate("containers")
                                },
                                onNavigateLogs = {
                                    navController.navigate("logs")
                                }
                            )
                        }

                        composable("containers") {
                            ContainersScreen(
                                containers = listOf(
                                    ContainerProfile(name = "Primary 64-bit Container", wineArch = "win64")
                                ),
                                onCreateNewContainer = {
                                    Toast.makeText(this@MainActivity, "New Container created", Toast.LENGTH_SHORT).show()
                                },
                                onDeleteContainer = {
                                    Toast.makeText(this@MainActivity, "Container deleted", Toast.LENGTH_SHORT).show()
                                },
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
