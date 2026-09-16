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
import org.windroid.core.graphics.DisplayResolution
import org.windroid.core.graphics.GraphicsDriverType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsSettingsScreen(
    onNavigateBack: () -> Unit
) {
    var selectedDriver by remember { mutableStateOf(GraphicsDriverType.TURNIP_ADRENO) }
    var selectedResolution by remember { mutableStateOf(DisplayResolution.RES_720P) }
    var enableDxvkHud by remember { mutableStateOf(true) }
    var enableAsyncShader by remember { mutableStateOf(true) }
    var enableVsync by remember { mutableStateOf(false) }
    var frameLimit by remember { mutableStateOf("60 FPS") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Graphics & Presentation Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            // Driver Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Vulkan / OpenGL Driver Backend", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    GraphicsDriverType.values().forEach { driver ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedDriver == driver),
                                onClick = { selectedDriver = driver }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(driver.displayName, color = Color.White, fontSize = 13.sp)
                                Text(driver.recommendedGpu, color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Virtual Desktop Resolution
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Virtual Desktop Display Resolution", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    DisplayResolution.values().forEach { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedResolution == res),
                                onClick = { selectedResolution = res }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(res.label, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Direct3D Translation (DXVK / VKD3D)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DirectX Translation & Shaders", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DXVK Performance HUD", color = Color.White, fontSize = 13.sp)
                            Text("Overlay displaying live FPS, memory, and frametimes", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(checked = enableDxvkHud, onCheckedChange = { enableDxvkHud = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Async Shader Compilation", color = Color.White, fontSize = 13.sp)
                            Text("Reduces stutter by compiling Vulkan pipelines in parallel", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(checked = enableAsyncShader, onCheckedChange = { enableAsyncShader = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("VSync Synchronization", color = Color.White, fontSize = 13.sp)
                            Text("Synchronize presentation to display refresh rate", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(checked = enableVsync, onCheckedChange = { enableVsync = it })
                    }
                }
            }

            // Frame Limiter
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Target Frame Rate Limiter", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("30 FPS", "45 FPS", "60 FPS", "Unlimited").forEach { limit ->
                            FilterChip(
                                selected = (frameLimit == limit),
                                onClick = { frameLimit = limit },
                                label = { Text(limit, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}
