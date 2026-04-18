package com.autowallpaper.changer.presentation.screens.library

import android.net.Uri
import android.provider.DocumentsContract
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.autowallpaper.changer.BuildConfig
import com.autowallpaper.changer.domain.model.WallpaperItem
import com.autowallpaper.changer.service.GalleryApiService
import kotlinx.coroutines.launch

// Helper function to build full URL from relative path
private fun buildFullUrl(relativePath: String): String {
    val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")
    return if (relativePath.startsWith("http")) relativePath else "$baseUrl$relativePath"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var onlineImages by remember { mutableStateOf<List<GalleryApiService.GalleryImage>>(emptyList()) }
    var isLoadingOnline by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<GalleryApiService.Category>>(emptyList()) }
    var categoryGroups by remember { mutableStateOf<List<Pair<String, GalleryApiService.CategoryGroup>>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<String?>("style") }
    var categoryNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var debugInfo by remember { mutableStateOf<String?>(null) }
    var previewOnlineImage by remember { mutableStateOf<GalleryApiService.GalleryImage?>(null) }

    // 資料夾選擇器（用於自動新增下載資料夾）
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            viewModel.addFolder(uri)
            viewModel.reloadImages()
        }
    }

    // 彈出資料夾選擇器
    fun launchFolderPicker() {
        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderPickerLauncher.launch(intent)
    }

    // 載入線上圖庫分類
    LaunchedEffect(Unit) {
        GalleryApiService.getCategories().onSuccess { response ->
            categories = response.getAllCategories()
            categoryGroups = response.getGroups()
            categoryNames = response.names ?: emptyMap()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("本地圖庫") },
                icon = { Icon(Icons.Default.Folder, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { 
                    selectedTab = 1
                    if (onlineImages.isEmpty()) {
                        isLoadingOnline = true
                        scope.launch {
                            GalleryApiService.getImages(category = selectedCategory).onSuccess {
                                onlineImages = it.images
                            }
                            isLoadingOnline = false
                        }
                    }
                },
                text = { Text("線上圖庫") },
                icon = { Icon(Icons.Default.Cloud, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> LocalLibraryContent(
                uiState = uiState,
                viewModel = viewModel
            )
            1 -> OnlineGalleryContent(
                images = onlineImages,
                categories = categories,
                categoryGroups = categoryGroups,
                categoryNames = categoryNames,
                selectedGroup = selectedGroup,
                selectedCategory = selectedCategory,
                isLoading = isLoadingOnline,
                debugMode = uiState.debugMode,
                debugInfo = debugInfo,
                onGroupSelected = { selectedGroup = it },
                onCategorySelected = { category ->
                    selectedCategory = category
                    debugInfo = "Debug: 切換分類中..."
                    isLoadingOnline = true
                    scope.launch {
                        val result = GalleryApiService.getImages(category = category)
                        result.onSuccess { response ->
                            onlineImages = response.images
                            debugInfo = "Debug: 分類 $category - 共 ${response.images.size} 張圖片\nURL: ${BuildConfig.API_BASE_URL}api/gallery"
                        }.onFailure { error ->
                            debugInfo = "Debug 失敗: ${error.message}\nURL: ${BuildConfig.API_BASE_URL}api/gallery"
                        }
                        isLoadingOnline = false
                    }
                },
                onPreview = { image -> previewOnlineImage = image },
                onRefresh = {
                    debugInfo = "Debug: 重新整理中..."
                    isLoadingOnline = true
                    scope.launch {
                        val result = GalleryApiService.getImages(category = selectedCategory)
                        result.onSuccess { response ->
                            onlineImages = response.images
                            debugInfo = "Debug: 成功! 共 ${response.images.size} 張圖片\nAPI: ${BuildConfig.API_BASE_URL}api/gallery?category=$selectedCategory"
                        }.onFailure { error ->
                            debugInfo = "Debug 失敗: ${error.message}\nAPI: ${BuildConfig.API_BASE_URL}api/gallery?category=$selectedCategory"
                        }
                        isLoadingOnline = false
                    }
                }
            )
        }
    }

    // 線上圖片預覽對話框
    if (previewOnlineImage != null) {
        var isDownloading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { previewOnlineImage = null },
            title = { Text(previewOnlineImage!!.title ?: "線上圖片") },
            text = {
                Column {
                    AsyncImage(
                        model = buildFullUrl(previewOnlineImage!!.fullUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "分類: ${previewOnlineImage!!.category}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDownloading = true
                        scope.launch {
                            GalleryApiService.downloadToGallery(
                                context = context,
                                imageUrl = previewOnlineImage!!.fullUrl,
                                filename = previewOnlineImage!!.filename
                            ).onSuccess { uri ->
                                previewOnlineImage = null
                                // 檢查是否已加入 AutoWallpaper 資料夾
                                if (viewModel.isAutoWallpaperFolderAdded()) {
                                    Toast.makeText(context, "已下載到 AutoWallpaper 資料夾", Toast.LENGTH_SHORT).show()
                                    viewModel.reloadImages()
                                } else {
                                    Toast.makeText(context, "已下載到 AutoWallpaper 資料夾，請選擇該資料夾以添加到本地圖庫", Toast.LENGTH_LONG).show()
                                    // 延遲彈出選擇器，確保 Toast 可以看到
                                    kotlinx.coroutines.delay(500)
                                    launchFolderPicker()
                                }
                            }.onFailure { error ->
                                isDownloading = false
                                when (error) {
                                    is GalleryApiService.ImageAlreadyExistsException -> {
                                        Toast.makeText(context, "這張圖片已經下載過了", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        Toast.makeText(context, "下載失敗: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下載")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewOnlineImage = null }) {
                    Text("關閉")
                }
            }
        )
    }
}

@Composable
private fun LocalLibraryContent(
    uiState: LibraryUiState,
    viewModel: LibraryViewModel
) {
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.addFolder(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    LocalImageGridItem(
                        image = image,
                        onClick = { viewModel.previewImage(image) },
                        onDelete = { 
                            viewModel.setDeleteImage(image)
                        }
                    )
                }
            }
        }
    }
    
    // 刪除確認 Dialog
    if (uiState.deleteImage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearDeleteImage() },
            title = { Text("刪除圖片") },
            text = { Text("確定要刪除這張圖片嗎？此操作無法撤銷。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deleted = viewModel.confirmDelete()
                        if (!deleted) {
                            Toast.makeText(context, "刪除失敗，可能需要更多權限", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearDeleteImage() }) {
                    Text("取消")
                }
            }
        )
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
}

@Composable
private fun OnlineGalleryContent(
    images: List<GalleryApiService.GalleryImage>,
    categories: List<GalleryApiService.Category>,
    categoryGroups: List<Pair<String, GalleryApiService.CategoryGroup>>,
    categoryNames: Map<String, String>,
    selectedGroup: String?,
    selectedCategory: String?,
    isLoading: Boolean,
    debugMode: Boolean,
    debugInfo: String?,
    onGroupSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPreview: (GalleryApiService.GalleryImage) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Debug info display
        if (debugMode && debugInfo != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = debugInfo,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "線上圖庫",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "重新整理")
            }
        }

        // 分類選擇器（雙層）
        if (categoryGroups.isNotEmpty()) {
            // 大分類下拉選單
            var expandedGroup by remember { mutableStateOf(false) }
            var expandedCategory by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 大分類下拉
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = categoryGroups.find { it.first == selectedGroup }?.second?.name ?: "選擇大分類",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { expandedGroup = true }) {
                                Icon(Icons.Default.ArrowDropDown, "下拉")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedGroup,
                        onDismissRequest = { expandedGroup = false }
                    ) {
                        categoryGroups.forEach { (key, group) ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    onGroupSelected(key)
                                    expandedGroup = false
                                }
                            )
                        }
                    }
                }
                
                // 小分類下拉
                val selectedGroupData = categoryGroups.find { it.first == selectedGroup }?.second
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedCategory?.let { categoryNames[it] ?: it } ?: "選擇小分類",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedGroup != null && selectedGroupData != null,
                        trailingIcon = {
                            IconButton(onClick = { if (selectedGroup != null) expandedCategory = true }) {
                                Icon(Icons.Default.ArrowDropDown, "下拉")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        // 加入「全部」選項
                        DropdownMenuItem(
                            text = { Text("全部") },
                            onClick = {
                                onCategorySelected(null)
                                expandedCategory = false
                            }
                        )
                        selectedGroupData?.categories?.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.name} (${cat.count})") },
                                onClick = {
                                    onCategorySelected(cat.id)
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Image Grid
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (images.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "線上圖庫為空",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "請先上傳圖片到後端",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "共 ${images.size} 張圖片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(images) { image ->
                    OnlineImageGridItem(
                        image = image,
                        onPreview = { onPreview(image) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineImageGridItem(
    image: GalleryApiService.GalleryImage,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clickable { onPreview() }
    ) {
        Box {
            AsyncImage(
                model = buildFullUrl(image.thumbnailUrl),
                contentDescription = image.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalImageGridItem(
    image: WallpaperItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clickable(onClick = onClick)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Box {
            AsyncImage(
                model = image.uri,
                contentDescription = image.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 長按選單
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("刪除", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}
