package org.windroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.windroid.core.container.ContainerProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(
    containers: List<ContainerProfile>,
    onCreateNewContainer: () -> Unit,
    onEditContainer: (ContainerProfile) -> Unit = {},
    onDeleteContainer: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Container Management", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewContainer,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+ New", modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(containers) { container ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                container.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            BadgeText(text = container.wineArch.uppercase())
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text("Resolution", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(container.screenResolution, fontSize = 13.sp)
                            }
                            Column {
                                Text("Graphics Driver", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(container.graphicsDriver, fontSize = 13.sp)
                            }
                            Column {
                                Text("Box64 Preset", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(container.box64Preset.name, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onEditContainer(container) }
                            ) {
                                Text("Configure", color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onDeleteContainer(container.id) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete Prefix")
                            }
                        }
                    }
                }
            }
        }
    }
}
