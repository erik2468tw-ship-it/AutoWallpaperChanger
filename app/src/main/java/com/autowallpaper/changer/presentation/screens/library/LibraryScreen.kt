package com.autowallpaper.changer.presentation.screens.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.autowallpaper.changer.domain.model.WallpaperItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.addFolder(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Folder Selection Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "圖庫管理",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { folderPicker.launch(null) }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "新增資料夾")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info text
        Text(
            text = "選擇圖片資料夾（主螢幕與鎖定螢幕共用）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Folders
        if (uiState.selectedFolders.isNotEmpty()) {
            Text(
                text = "已選擇的資料夾",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            uiState.selectedFolders.forEach { folderUri ->
                val folderName = remember(folderUri) {
                    try {
                        val uri = Uri.parse(folderUri)
                        DocumentFile.fromTreeUri(context, uri)?.name
                            ?: folderUri.substringAfterLast("/").ifEmpty { folderUri }
                    } catch (e: Exception) {
                        folderUri.substringAfterLast("/").ifEmpty { folderUri }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { viewModel.removeFolder(folderUri) }) {
                        Icon(Icons.Default.Delete, contentDescription = "移除")
                    }
                }
            }
            HorizontalDivider()
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Image Grid
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.images.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "尚無圖片",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "新增資料夾以開始",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "共 ${uiState.images.size} 張圖片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.images) { image ->
                    ImageGridItem(
                        image = image,
                        onClick = { viewModel.previewImage(image) }
                    )
                }
            }
        }
    }

    // Preview Dialog
    if (uiState.previewImage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPreview() },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPreview() }) {
                    Text("關閉")
                }
            },
            text = {
                AsyncImage(
                    model = uiState.previewImage!!.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f),
                    contentScale = ContentScale.Crop
                )
            }
        )
    }

    // Error snackbar
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun ImageGridItem(
    image: WallpaperItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
