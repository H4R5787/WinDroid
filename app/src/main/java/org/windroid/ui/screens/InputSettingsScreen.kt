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
import org.windroid.core.input.MouseMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSettingsScreen(
    onNavigateBack: () -> Unit
) {
    var selectedMouseMode by remember { mutableStateOf(MouseMode.RELATIVE_TRACKPAD) }
    var touchSensitivity by remember { mutableStateOf(1.0f) }
    var virtualGamepadEnabled by remember { mutableStateOf(true) }
    var overlayOpacity by remember { mutableStateOf(0.7f) }
    var analogDeadzone by remember { mutableStateOf(0.15f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input & Controls Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            // Mouse Mode
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mouse & Pointer Emulation Mode", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    MouseMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedMouseMode == mode),
                                onClick = { selectedMouseMode = mode }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(mode.displayName, color = Color.White, fontSize = 13.sp)
                                Text(mode.description, color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Touch Sensitivity
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Touch & Cursor Sensitivity", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                        Text("${String.format("%.1f", touchSensitivity)}x", color = Color.White, fontSize = 13.sp)
                    }
                    Slider(
                        value = touchSensitivity,
                        onValueChange = { touchSensitivity = it },
                        valueRange = 0.5f..3.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF)
                        )
                    )
                }
            }

            // Virtual Gamepad Overlay Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("On-Screen Virtual Gamepad", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Render D-pad, dual analog thumbsticks, and ABXY buttons", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(checked = virtualGamepadEnabled, onCheckedChange = { virtualGamepadEnabled = it })
                    }

                    if (virtualGamepadEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Button Opacity", color = Color.LightGray, fontSize = 12.sp)
                            Text("${(overlayOpacity * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                        }
                        Slider(
                            value = overlayOpacity,
                            onValueChange = { overlayOpacity = it },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stick Deadzone", color = Color.LightGray, fontSize = 12.sp)
                            Text("${(analogDeadzone * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                        }
                        Slider(
                            value = analogDeadzone,
                            onValueChange = { analogDeadzone = it },
                            valueRange = 0.05f..0.35f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            )
                        )
                    }
                }
            }

            // Physical Gamepad Status
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("External Game Controllers", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bluetooth & USB Gamepads (Xbox, PlayStation, Generic HID) automatically map to Windows XInput.", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Badge(containerColor = Color(0xFF262B34)) {
                        Text("XInput Driver: Ready", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
