package com.autowallpaper.changer.presentation.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autowallpaper.changer.util.ROMUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "設定",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Appearance Section
        SettingsSectionHeader(title = "外觀")
        SettingsCard {
            SettingsSwitchItem(
                title = "深色主題",
                subtitle = "使用深色配色",
                icon = Icons.Default.DarkMode,
                checked = uiState.isDarkTheme,
                onCheckedChange = { viewModel.setDarkTheme(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Power Section
        SettingsSectionHeader(title = "電力")
        SettingsCard {
            SettingsSwitchItem(
                title = "低電量暫停",
                subtitle = "電量低於 ${uiState.lowBatteryThreshold}% 時暫停自動更換",
                icon = Icons.Default.BatteryAlert,
                checked = uiState.lowBatteryPause,
                onCheckedChange = { viewModel.setLowBatteryPause(it) }
            )
            if (uiState.lowBatteryPause) {
                SettingsClickableItem(
                    title = "低電量閾值",
                    subtitle = "目前: ${uiState.lowBatteryThreshold}%",
                    icon = Icons.Default.BatteryFull,
                    onClick = { showBatteryDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cache Section
        SettingsSectionHeader(title = "快取")
        SettingsCard {
            SettingsSwitchItem(
                title = "自動清理",
                subtitle = "自動刪除過期快取",
                icon = Icons.Default.DeleteSweep,
                checked = uiState.autoClearCache,
                onCheckedChange = { viewModel.setAutoClearCache(it) }
            )
            if (uiState.autoClearCache) {
                SettingsClickableItem(
                    title = "保留天數",
                    subtitle = "快取保留 ${uiState.cacheMaxDays} 天後刪除",
                    icon = Icons.Default.DateRange,
                    onClick = { showCacheDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Section
        SettingsSectionHeader(title = "系統")
        SettingsCard {
            SettingsClickableItem(
                title = "自動啟動設定",
                subtitle = "開機後自動啟動服務",
                icon = Icons.Default.Settings,
                onClick = {
                    ROMUtils.openAutoStartSettings(context)
                }
            )
            HorizontalDivider()
            SettingsClickableItem(
                title = "應用程式設定",
                subtitle = "開啟系統應用程式設定",
                icon = Icons.Default.Apps,
                onClick = {
                    ROMUtils.openAppSettings(context)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Debug Section
        SettingsSectionHeader(title = "開發者選項")
        SettingsCard {
            SettingsSwitchItem(
                title = "DEBUG 模式",
                subtitle = "顯示詳細除錯資訊",
                icon = Icons.Default.BugReport,
                checked = uiState.debugMode,
                onCheckedChange = { viewModel.setDebugMode(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Help Section
        SettingsSectionHeader(title = "使用說明")
        SettingsCard {
            SettingsClickableItem(
                title = "軟體使用說明",
                subtitle = "查看操作指南",
                icon = Icons.Default.Help,
                onClick = { showHelpDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SettingsSectionHeader(title = "關於")
        SettingsCard {
            SettingsClickableItem(
                title = "版本",
                subtitle = "${uiState.appVersion}",
                icon = Icons.Default.Info,
                onClick = { }
            )
        }
    }

    // Battery Threshold Dialog
    if (showBatteryDialog) {
        var batteryValue by remember { mutableStateOf(uiState.lowBatteryThreshold.toString()) }

        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text("低電量閾值") },
            text = {
                OutlinedTextField(
                    value = batteryValue,
                    onValueChange = { batteryValue = it.filter { c -> c.isDigit() } },
                    label = { Text("電量百分比") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        batteryValue.toIntOrNull()?.let { value ->
                            if (value in 5..50) {
                                viewModel.setLowBatteryThreshold(value)
                            }
                        }
                        showBatteryDialog = false
                    }
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Cache Days Dialog
    if (showCacheDialog) {
        var cacheValue by remember { mutableStateOf(uiState.cacheMaxDays.toString()) }

        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("快取保留天數") },
            text = {
                OutlinedTextField(
                    value = cacheValue,
                    onValueChange = { cacheValue = it.filter { c -> c.isDigit() } },
                    label = { Text("天數") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cacheValue.toIntOrNull()?.let { days ->
                            if (days in 1..30) {
                                viewModel.setCacheMaxDays(days)
                            }
                        }
                        showCacheDialog = false
                    }
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("軟體使用說明") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("📱 自動換桌布", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. 選擇要使用的圖片資料夾\n2. 設定自動更換間隔時間\n3. 開啟自動更換功能")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("🎯 懸浮球快速換圖", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. 在首頁開啟懸浮球功能\n2. 首次使用需授權「顯示在其他應用程式上」\n3. 點擊懸浮球立即更換主螢幕桌布\n4. 拖曳懸浮球可移動位置")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("🖼️ 線上圖庫下載", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. 進入「圖庫」頁面\n2. 瀏覽線上圖庫，點擊圖片可下載\n3. 下載後圖片會存到「AutoWallpaper」資料夾\n4. 建議將 AutoWallpaper 資料夾加入圖片庫")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("📁 資料夾設定", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. 加入資料夾後，圖片會出現在本地圖庫\n2. 可刪除已加入的資料夾\n3. 下載的圖片會自動添加到本地圖庫")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("🖼️ 電子相簿", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. 點擊電子相簿進入播放模式\n2. 點擊螢幕可暫停/播放\n3. 圖片會隨機播放")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("⚙️ 權限說明", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 儲存權限：用於讀取圖片\n• 懸浮球權限：用於顯示懸浮球\n• 開機權限：開機後自動啟動服務")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("確定")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
