package org.windroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.windroid.core.graphics.DisplayResolution
import org.windroid.core.graphics.GraphicsDriverType

enum class ContainerTab(val title: String) {
    GENERAL("General"),
    GRAPHICS("Graphics"),
    DYNAREC("Box64 Dynarec"),
    DRIVES("Drives & Storage")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerEditScreen(
    initialProfile: ContainerProfile,
    onSave: (ContainerProfile) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ContainerTab.GENERAL) }
    var name by remember { mutableStateOf(initialProfile.name) }
    var wineArch by remember { mutableStateOf(initialProfile.wineArch) }
    var screenResolution by remember { mutableStateOf(initialProfile.screenResolution) }
    var graphicsDriver by remember { mutableStateOf(initialProfile.graphicsDriver) }
    var box64Preset by remember { mutableStateOf(initialProfile.box64Preset) }
    var showFps by remember { mutableStateOf(initialProfile.showFpsCounter) }
    var isolateNetwork by remember { mutableStateOf(initialProfile.isolateNetwork) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Container: $name", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel", color = MaterialTheme.colorScheme.outline)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSave(
                                initialProfile.copy(
                                    name = name,
                                    wineArch = wineArch,
                                    screenResolution = screenResolution,
                                    graphicsDriver = graphicsDriver,
                                    box64Preset = box64Preset,
                                    showFpsCounter = showFps,
                                    isolateNetwork = isolateNetwork
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF16191E))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Tab Bar
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF1B1F26),
                contentColor = Color(0xFF00E5FF)
            ) {
                ContainerTab.entries.forEach { tab ->
                    Tab(
                        selected = (selectedTab == tab),
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    ContainerTab.GENERAL -> {
                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Container Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text("Wine Prefix Architecture", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilterChip(
                                    selected = (wineArch == "win64"),
                                    onClick = { wineArch = "win64" },
                                    label = { Text("64-bit (win64 - Recommended)") }
                                )
                                FilterChip(
                                    selected = (wineArch == "win32"),
                                    onClick = { wineArch = "win32" },
                                    label = { Text("32-bit (win32 Legacy)") }
                                )
                            }
                        }
                        item {
                            Text("Screen Resolution", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DisplayResolution.entries.forEach { res ->
                                    val formatted = res.formattedString
                                    FilterChip(
                                        selected = (screenResolution == formatted),
                                        onClick = { screenResolution = formatted },
                                        label = { Text(res.label) }
                                    )
                                }
                            }
                        }
                    }

                    ContainerTab.GRAPHICS -> {
                        item {
                            Text("Vulkan & GPU Acceleration Driver", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                GraphicsDriverType.entries.forEach { drv ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (graphicsDriver == drv.displayName)
                                                Color(0xFF00E5FF).copy(alpha = 0.15f)
                                            else Color(0xFF1E222B)
                                        ),
                                        onClick = { graphicsDriver = drv.displayName }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(drv.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(drv.recommendedGpu, fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Display DXVK Performance HUD", fontWeight = FontWeight.SemiBold)
                                Switch(checked = showFps, onCheckedChange = { showFps = it })
                            }
                        }
                    }

                    ContainerTab.DYNAREC -> {
                        item {
                            Text("Box64 Dynamic Binary Translation Preset", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box64Preset.entries.forEach { preset ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (box64Preset == preset)
                                            Color(0xFF00E5FF).copy(alpha = 0.15f)
                                        else Color(0xFF1E222B)
                                    ),
                                    onClick = { box64Preset = preset }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(preset.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        preset.envVars.forEach { (k, v) ->
                                            Text("$k=$v", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ContainerTab.DRIVES -> {
                        item {
                            Text("Storage Access & Drive Mapping", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("C:  →  Container Internal (drive_c)", fontWeight = FontWeight.SemiBold)
                                    Text("D:  →  Android User Storage (Games Folder)", fontWeight = FontWeight.SemiBold)
                                    Text("Z:  →  Rootfs Binaries (Read-Only Sandbox)", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
