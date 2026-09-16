package org.windroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.windroid.core.nativebridge.NativeBridge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val detectedCpu = NativeBridge.safeGetCpuArchitecture()
    val detectedGpu = NativeBridge.safeDetectGpu()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About WinDroid", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            // Header Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "WINDROID",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Windows Application Compatibility Runtime for Android",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Badge(containerColor = Color(0xFF262B34)) {
                        Text("v0.1.0-alpha • ARM64 Native", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }
                }
            }

            // Hardware Telemetry
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DEVICE HARDWARE PROFILE", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("CPU Architecture: $detectedCpu", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("GPU Acceleration: $detectedGpu", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Storage Isolation: Android Scoped Storage (SAF)", color = Color.White, fontSize = 13.sp)
                }
            }

            // Open Source Compliance & Legal Notice
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("OPEN SOURCE LICENSES & ATTRIBUTION", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val licenses = listOf(
                        "Wine" to "GNU LGPL v2.1+ • Windows API translation & Win32 subsystem",
                        "Box64 / Box86" to "MIT License • Dynamic binary translation & Dynarec JIT",
                        "DXVK" to "zlib/libpng License • Direct3D 9/10/11 to Vulkan translation",
                        "VKD3D-Proton" to "GNU LGPL v2.1 • Direct3D 12 to Vulkan translation",
                        "Mesa Turnip / Panfrost" to "MIT / X11 License • Open source Vulkan drivers for Adreno/Mali",
                        "PRoot" to "GNU GPL v2.0 • Chroot and mount isolation without root privileges"
                    )

                    licenses.forEach { (name, license) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(license, color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "WinDroid does not distribute or require any proprietary Microsoft Windows files. All Windows APIs are implemented via clean-room open-source implementations.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
