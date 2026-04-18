package com.autowallpaper.changer.presentation.screens.home

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
import com.autowallpaper.changer.presentation.components.QuickActionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isSchedulerActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (uiState.isSchedulerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isSchedulerActive) "自動換桌布已啟用" else "自動換桌布已停用",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "間隔: ${uiState.intervalDisplay}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (uiState.lastChangeTime > 0) {
                    Text(
                        text = "上次更換: ${uiState.lastChangeTimeDisplay}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions
        Text(
            text = "快速操作",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                title = "更換主螢幕",
                icon = Icons.Default.Home,
                onClick = { viewModel.changeHomeScreenNow() },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "更換鎖屏",
                icon = Icons.Default.Lock,
                onClick = { viewModel.changeLockScreenNow() },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "隨機更換",
                icon = Icons.Default.Shuffle,
                onClick = { viewModel.changeRandomNow() },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                title = "更換兩者",
                icon = Icons.Default.Wallpaper,
                onClick = { viewModel.changeBothNow() },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "電子相簿",
                icon = Icons.Default.PhotoLibrary,
                onClick = { viewModel.openSlideshow() },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shuffle Toggle
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
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "隨機更換",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (uiState.shuffleEnabled) "隨機選擇圖片" else "依序選擇圖片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.shuffleEnabled,
                    onCheckedChange = { viewModel.setShuffle(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Bubble Toggle
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
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "懸浮球快速換圖",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (uiState.quickChangeBubbleEnabled) "已啟用" else "點擊懸浮球立即換圖",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.quickChangeBubbleEnabled,
                    onCheckedChange = { viewModel.setQuickChangeBubble(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics
        Text(
            text = "統計",
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("可用圖片")
                    Text("${uiState.availableImages} 張")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("選擇的資料夾")
                    Text("${uiState.selectedFolders} 個")
                }
            }
        }
    }
}
