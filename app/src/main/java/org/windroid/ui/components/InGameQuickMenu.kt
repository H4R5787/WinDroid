package org.windroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.windroid.core.input.MouseMode

/**
 * Slide-out in-game overlay menu accessible during execution.
 * Provides live controls for FPS HUD, input mode switching, virtual keyboard, and safe process exit.
 */
@Composable
fun InGameQuickMenu(
    isOpen: Boolean,
    currentMouseMode: MouseMode,
    isFpsCounterEnabled: Boolean,
    onToggleFps: (Boolean) -> Unit,
    onChangeMouseMode: (MouseMode) -> Unit,
    onSummonVirtualKeyboard: () -> Unit,
    onForceKillWine: () -> Unit,
    onCloseMenu: () -> Unit
) {
    if (!isOpen) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .width(320.dp)
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Quick Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                        IconButton(onClick = onCloseMenu) {
                            Text("✕", color = Color.White, fontSize = 16.sp)
                        }
                    }

                    Divider(color = Color(0xFF2A2E35))

                    // FPS Counter Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Performance HUD", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("FPS, Frame Time & VRAM", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isFpsCounterEnabled,
                            onCheckedChange = onToggleFps,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                        )
                    }

                    // Input Mode Selector
                    Text("Input Method", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MouseMode.entries.forEach { mode ->
                            val isSelected = (mode == currentMouseMode)
                            Button(
                                onClick = { onChangeMouseMode(mode) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF23272F),
                                    contentColor = if (isSelected) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(mode.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Virtual Keyboard Trigger
                    OutlinedButton(
                        onClick = onSummonVirtualKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Summon Soft Keyboard", fontSize = 13.sp)
                    }
                }

                // Emergency Exit / Wine Kill
                Button(
                    onClick = onForceKillWine,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Force Stop Wine (Kill Process)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
