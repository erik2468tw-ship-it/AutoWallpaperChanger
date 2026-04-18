package com.autowallpaper.changer.presentation.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Enable/Disable Switch
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "自動排程",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (uiState.isEnabled) "已啟用" else "已停用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.isEnabled,
                    onCheckedChange = { viewModel.setEnabled(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interval Selection
        Text(
            text = "更換間隔",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Preset intervals (in seconds)
                val intervals = listOf(
                    5 to "5 秒",
                    30 to "30 秒",
                    60 to "1 分鐘",
                    300 to "5 分鐘",
                    900 to "15 分鐘",
                    1800 to "30 分鐘",
                    3600 to "1 小時"
                )

                intervals.forEach { (seconds, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !uiState.isCustomInterval && uiState.intervalSeconds == seconds,
                            onClick = {
                                viewModel.setInterval(seconds)
                                viewModel.setCustomInterval(false)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }

                // Custom interval (in hours)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.isCustomInterval,
                        onClick = {
                            viewModel.setCustomInterval(true)
                            showCustomDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isCustomInterval) {
                            val hours = uiState.customIntervalSeconds / 3600
                            "自定義: ${hours} 小時"
                        } else "自定義(小時)..."
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState.isCustomInterval) {
                        IconButton(onClick = { showCustomDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "編輯")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Selection
        Text(
            text = "更換目標",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.changeHomeScreen,
                        onCheckedChange = { viewModel.setChangeHomeScreen(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("主螢幕")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.changeLockScreen,
                        onCheckedChange = { viewModel.setChangeLockScreen(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("鎖定螢幕")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Slideshow Interval Selection
        Text(
            text = "電子相簿間隔",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val slideshowIntervals = listOf(
                    3 to "3 秒",
                    5 to "5 秒",
                    10 to "10 秒",
                    15 to "15 秒"
                )

                slideshowIntervals.forEach { (seconds, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.slideshowIntervalSeconds == seconds,
                            onClick = { viewModel.setSlideshowInterval(seconds) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        }
    }

    // Custom Interval Dialog (in hours)
    if (showCustomDialog) {
        var customValue by remember { mutableStateOf((uiState.customIntervalSeconds / 3600).toString()) }

        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("自定義間隔 (小時)") },
            text = {
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it.filter { c -> c.isDigit() } },
                    label = { Text("小時") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customValue.toIntOrNull()?.let { hours ->
                            if (hours > 0) {
                                // 轉換為秒並儲存
                                val seconds = hours * 3600
                                viewModel.setCustomIntervalSeconds(seconds)
                                viewModel.setCustomInterval(true)
                            }
                        }
                        showCustomDialog = false
                    }
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
