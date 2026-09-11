package org.windroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.windroid.core.runtime.RuntimeLogEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    logs: List<RuntimeLogEntry>,
    onClearLogs: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wine & Box64 Runtime Logs") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back", color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    TextButton(onClick = onClearLogs) {
                        Text("Clear", color = MaterialTheme.colorScheme.outline)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F0F0F))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { entry ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    val color = when (entry.source) {
                        "WINE" -> Color(0xFF81C784)
                        "BOX64" -> Color(0xFFFFB74D)
                        "DXVK" -> Color(0xFF64B5F6)
                        else -> Color(0xFFB0BEC5)
                    }
                    Text(
                        "[${entry.source}] ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = color
                    )
                    Text(
                        entry.message,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFEEEEEE)
                    )
                }
            }
        }
    }
}
