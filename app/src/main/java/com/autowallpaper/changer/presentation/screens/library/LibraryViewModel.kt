package com.autowallpaper.changer.presentation.screens.library

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.domain.model.WallpaperItem
import com.autowallpaper.changer.domain.usecase.GetImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val selectedFolders: List<String> = emptyList(),
    val images: List<WallpaperItem> = emptyList(),
    val isLoading: Boolean = false,
    val previewImage: WallpaperItem? = null,
    val message: String? = null,
    val debugMode: Boolean = false,
    val deleteImage: WallpaperItem? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getImagesUseCase: GetImagesUseCase,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
        loadImages()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update { it.copy(
                    selectedFolders = settings.selectedHomeFolders,
                    debugMode = settings.debugMode
                )}
            }
        }
    }

    private fun loadImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val images = getImagesUseCase.getWallpaperImages()
                _uiState.update { it.copy(images = images, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "載入圖片失敗: ${e.message}", isLoading = false) }
            }
        }
    }
    
    fun reloadImages() {
        loadImages()
    }

    fun addFolder(folderUri: Uri) {
        viewModelScope.launch {
            try {
                // Take persistable URI permission for SAF access (包含寫入權限)
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(folderUri, flags)
            } catch (e: Exception) {
                // Permission might not be persistable, continue anyway
                e.printStackTrace()
            }

            val currentFolders = _uiState.value.selectedFolders.toMutableList()
            val uriString = folderUri.toString()
            if (!currentFolders.contains(uriString)) {
                currentFolders.add(uriString)
                // Single shared folder for all wallpapers
                settingsDataStore.updateSelectedHomeFolders(currentFolders)
                settingsDataStore.updateSelectedLockFolders(currentFolders)
                _uiState.update { it.copy(selectedFolders = currentFolders) }
                loadImages()
            }
        }
    }

    fun removeFolder(folder: String) {
        viewModelScope.launch {
            val currentFolders = _uiState.value.selectedFolders.toMutableList()
            currentFolders.remove(folder)
            // Update both to keep them in sync
            settingsDataStore.updateSelectedHomeFolders(currentFolders)
            settingsDataStore.updateSelectedLockFolders(currentFolders)
            _uiState.update { it.copy(selectedFolders = currentFolders) }
            loadImages()
        }
    }

    fun previewImage(image: WallpaperItem) {
        _uiState.update { it.copy(previewImage = image) }
    }

    fun dismissPreview() {
        _uiState.update { it.copy(previewImage = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    fun isAutoWallpaperFolderAdded(): Boolean {
        // 檢查 selectedFolders 中是否已包含 AutoWallpaper 相關路徑
        val selectedFolders = _uiState.value.selectedFolders
        return selectedFolders.any { folder ->
            folder.contains("AutoWallpaper", ignoreCase = true) ||
            folder.contains("Pictures", ignoreCase = true)
        }
    }
    
    fun deleteImage(image: WallpaperItem): Boolean {
        // 使用 DocumentFile 來刪除 SAF 管理的檔案
        return try {
            val documentFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, image.uri)
            if (documentFile != null && documentFile.exists()) {
                val deleted = documentFile.delete()
                if (deleted) {
                    loadImages()
                }
                deleted
            } else {
                // 如果 DocumentFile 無效，嘗試用 contentResolver
                val deleted = context.contentResolver.delete(image.uri, null, null) > 0
                if (deleted) {
                    loadImages()
                }
                deleted
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun setDeleteImage(image: WallpaperItem) {
        _uiState.update { it.copy(deleteImage = image) }
    }
    
    fun clearDeleteImage() {
        _uiState.update { it.copy(deleteImage = null) }
    }
    
    fun confirmDelete(): Boolean {
        val imageToDelete = _uiState.value.deleteImage ?: return false
        val result = deleteImage(imageToDelete)
        clearDeleteImage()
        return result
    }
    
    fun setWallpaperFromLocal(image: WallpaperItem, target: String) {
        viewModelScope.launch {
            try {
                val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                val flag = when (target) {
                    "home" -> android.app.WallpaperManager.FLAG_SYSTEM
                    "lock" -> android.app.WallpaperManager.FLAG_LOCK
                    else -> 0
                }
                
                context.contentResolver.openInputStream(image.uri)?.use { input ->
                    wallpaperManager.setStream(input, null, true, flag)
                }
                _uiState.update { it.copy(message = "已設為${if (target == "home") "主螢幕" else "鎖屏"}桌布") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "設定桌布失敗: ${e.message}") }
            }
        }
    }
}
