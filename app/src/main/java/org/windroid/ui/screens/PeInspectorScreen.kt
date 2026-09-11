package org.windroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.windroid.core.pe.PeInspectionResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeInspectorScreen(
    result: PeInspectionResult,
    onLaunch: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Executable Compatibility Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Target: ${result.architecture.displayName}", fontSize = 12.sp)
                        Text("Subsystem: ${result.subsystem.name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Button(
                        onClick = onLaunch,
                        enabled = result.isValidPe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Run in Container", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isValidPe)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            result.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Size: ${result.fileSize / 1024} KB | Valid PE: ${result.isValidPe}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!result.isValidPe && result.errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Error: ${result.errorMessage}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Text("Runtime Architecture & Translation", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReportRow("Architecture", result.architecture.displayName)
                        ReportRow("Binary Bitness", if (result.is64Bit) "64-bit (x86_64)" else "32-bit (x86)")
                        ReportRow("Translation Engine", when {
                            result.requiresBox64 -> "Box64 (x86_64 Dynarec)"
                            result.requiresBox86 -> "Box86 (x86 Dynarec)"
                            else -> "Native ARM64 Execution"
                        })
                        ReportRow("Recommended Wine Arch", result.recommendedWinePrefixArch.uppercase())
                        ReportRow("Recommended Dynarec Preset", result.recommendedBox64Preset)
                    }
                }
            }

            item {
                Text("Graphics & API Requirements", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReportRow("Graphics Backend", result.detectedGraphics.displayName)
                        ReportRow("Recommended Translator", result.detectedGraphics.recommendedTranslator)
                        ReportRow(".NET Framework", if (result.isDotNetAssembly) "Required (Wine Mono / DotNet)" else "None")
                        ReportRow("Entry Point RVA", "0x%X".format(result.entryPointRva))
                        ReportRow("Image Base", "0x%X".format(result.imageBase))
                    }
                }
            }

            item {
                Text("Imported Dynamic Link Libraries (${result.importedDlls.size})", fontWeight = FontWeight.Bold)
            }

            items(result.importedDlls) { dll ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(dll, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        val category = when {
                            dll.startsWith("d3d", ignoreCase = true) || dll.startsWith("dx", ignoreCase = true) -> "DirectX"
                            dll.contains("vulkan", ignoreCase = true) -> "Vulkan"
                            dll.contains("opengl", ignoreCase = true) -> "OpenGL"
                            dll.contains("xinput", ignoreCase = true) -> "Gamepad"
                            dll.startsWith("kernel32", ignoreCase = true) || dll.startsWith("ntdll", ignoreCase = true) -> "NT Core"
                            else -> "Win32 DLL"
                        }
                        Text(category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
