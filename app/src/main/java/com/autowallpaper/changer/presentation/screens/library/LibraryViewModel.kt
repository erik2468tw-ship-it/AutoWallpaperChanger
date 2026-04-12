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
    val message: String? = null
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
                    selectedFolders = settings.selectedHomeFolders
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

    fun addFolder(folderUri: Uri) {
        viewModelScope.launch {
            try {
                // Take persistable URI permission for SAF access
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
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
}
