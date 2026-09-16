package org.windroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.windroid.core.container.Box64Preset
import org.windroid.core.container.ContainerProfile
import org.windroid.core.graphics.GraphicsDriverType
import org.windroid.core.pe.PeInspectionResult
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppWizardScreen(
    inspectedFile: File?,
    peResult: PeInspectionResult?,
    containers: List<ContainerProfile>,
    onSelectFile: () -> Unit,
    onSaveAndLaunch: (title: String, containerId: String, preset: Box64Preset, driver: GraphicsDriverType, launchNow: Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf(inspectedFile?.nameWithoutExtension ?: "New Application") }
    var selectedContainerId by remember { mutableStateOf(containers.firstOrNull()?.id ?: "default-x64") }
    var selectedPreset by remember { mutableStateOf(Box64Preset.BALANCED) }
    var selectedDriver by remember { mutableStateOf(GraphicsDriverType.TURNIP_ADRENO) }
    var confirmedSecurity by remember { mutableStateOf(false) }

    LaunchedEffect(inspectedFile) {
        if (inspectedFile != null) {
            title = inspectedFile.nameWithoutExtension.replace("_", " ").replace("-", " ").capitalize()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Windows Application", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 20.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0F12),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0D0F12))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step 1: Select Binary
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Executable Binary", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (inspectedFile != null) {
                        Text("Selected: ${inspectedFile.name}", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Path: ${inspectedFile.absolutePath}", color = Color.Gray, fontSize = 12.sp)
                        if (peResult != null) {
                            Text("Detected: ${peResult.targetArchitecture} • ${peResult.subsystem}", color = Color(0xFF69F0AE), fontSize = 12.sp)
                        }
                    } else {
                        Text("No executable selected yet.", color = Color.LightGray, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onSelectFile,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262B34))
                    ) {
                        Text("Browse .EXE / .MSI", color = Color.White)
                    }
                }
            }

            // Step 2: Application Title
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Application Name", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF262B34)
                        )
                    )
                }
            }

            // Step 3: Container Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("3. Target Wine Container", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    containers.forEach { container ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedContainerId == container.id),
                                onClick = { selectedContainerId = container.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(container.name, color = Color.White, fontSize = 14.sp)
                                Text("${container.wineArch} • ${container.screenResolution}", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Step 4: Graphics & Emulation
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("4. Performance & Graphics Backend", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Box64 Dynarec Preset:", color = Color.LightGray, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box64Preset.values().forEach { preset ->
                            FilterChip(
                                selected = (selectedPreset == preset),
                                onClick = { selectedPreset = preset },
                                label = { Text(preset.name, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Graphics Driver:", color = Color.LightGray, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = (selectedDriver == GraphicsDriverType.TURNIP_ADRENO),
                            onClick = { selectedDriver = GraphicsDriverType.TURNIP_ADRENO },
                            label = { Text("Turnip", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = (selectedDriver == GraphicsDriverType.SYSTEM_VULKAN),
                            onClick = { selectedDriver = GraphicsDriverType.SYSTEM_VULKAN },
                            label = { Text("System Vulkan", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = (selectedDriver == GraphicsDriverType.VIRGL),
                            onClick = { selectedDriver = GraphicsDriverType.VIRGL },
                            label = { Text("VirGL Fallback", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Step 5: Security Consent
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF231916)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚠️ Security & Isolation Notice", fontWeight = FontWeight.Bold, color = Color(0xFFFFAB40), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Windows executables execute inside an isolated PRoot sandbox with access limited strictly to container virtual drives (C:, D:). Never run software from untrusted sources.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = confirmedSecurity,
                            onCheckedChange = { confirmedSecurity = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I understand and trust this executable", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (inspectedFile != null && confirmedSecurity) {
                            onSaveAndLaunch(title, selectedContainerId, selectedPreset, selectedDriver, false)
                        }
                    },
                    enabled = (inspectedFile != null && confirmedSecurity),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262B34))
                ) {
                    Text("Save to Library")
                }

                Button(
                    onClick = {
                        if (inspectedFile != null && confirmedSecurity) {
                            onSaveAndLaunch(title, selectedContainerId, selectedPreset, selectedDriver, true)
                        }
                    },
                    enabled = (inspectedFile != null && confirmedSecurity),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Launch Now", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
