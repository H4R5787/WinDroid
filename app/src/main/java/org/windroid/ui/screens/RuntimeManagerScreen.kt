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
import org.windroid.core.runtime.RuntimeStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeManagerScreen(
    status: RuntimeStatus,
    onVerifyIntegrity: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Runtime & Subsystem Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            // Rootfs Environment Card
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
                            Text("PRoot Linux Userspace", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text(status.rootfsPath, color = Color.Gray, fontSize = 11.sp)
                        }
                        Badge(containerColor = if (status.isRootfsInstalled) Color(0xFF00C853) else Color(0xFFFFAB40)) {
                            Text(
                                if (status.isRootfsInstalled) "ACTIVE" else "UNINITIALIZED",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Disk Footprint: ${status.rootfsSizeMb} MB", color = Color.LightGray, fontSize = 12.sp)
                        Text("Host Architecture: aarch64", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }

            // Subsystem Components
            Text("RUNTIME SUBSYSTEMS & LIBRARIES", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 12.sp)

            status.components.forEach { comp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(comp.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(comp.description, color = Color.Gray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Version: ${comp.installedVersion ?: comp.availableVersion}", color = Color(0xFF00E5FF), fontSize = 11.sp)
                        }
                        Badge(containerColor = if (comp.isInstalled) Color(0xFF00E5FF) else Color(0xFF757575)) {
                            Text(
                                if (comp.isInstalled) "INSTALLED" else "AVAILABLE",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Actions
            Button(
                onClick = onVerifyIntegrity,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262B34))
            ) {
                Text("Verify Runtime Binary Signatures", color = Color.White)
            }
        }
    }
}
