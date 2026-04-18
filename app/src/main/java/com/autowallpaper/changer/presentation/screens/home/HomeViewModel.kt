package com.autowallpaper.changer.presentation.screens.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.domain.usecase.ChangeWallpaperUseCase
import com.autowallpaper.changer.domain.usecase.GetImagesUseCase
import com.autowallpaper.changer.presentation.screens.slideshow.SlideshowActivity
import com.autowallpaper.changer.service.WallpaperScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isSchedulerActive: Boolean = false,
    val intervalDisplay: String = "30 分鐘",
    val lastChangeTime: Long = 0,
    val lastChangeTimeDisplay: String = "-",
    val availableImages: Int = 0,
    val selectedFolders: Int = 0,
    val shuffleEnabled: Boolean = true,
    val quickChangeBubbleEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val changeWallpaperUseCase: ChangeWallpaperUseCase,
    private val getImagesUseCase: GetImagesUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val wallpaperScheduler: WallpaperScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                val folderCount = settings.selectedHomeFolders.size
                _uiState.update { it.copy(
                    selectedFolders = folderCount,
                    intervalDisplay = formatInterval(settings.scheduleIntervalSeconds),
                    shuffleEnabled = settings.shuffleEnabled,
                    quickChangeBubbleEnabled = settings.quickChangeBubbleEnabled,
                    isSchedulerActive = settings.scheduleIntervalSeconds > 0
                )}
                loadData()
            }
        }

        viewModelScope.launch {
            settingsDataStore.lastChangeTime.collect { time ->
                val display = if (time > 0) {
                    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(time))
                } else {
                    "-"
                }
                _uiState.update { it.copy(
                    lastChangeTime = time,
                    lastChangeTimeDisplay = display
                )}
            }
        }
    }

    private fun formatInterval(seconds: Int): String {
        return when {
            seconds < 60 -> "$seconds 秒"
            seconds < 3600 -> "${seconds / 60} 分鐘"
            else -> "${seconds / 3600} 小時"
        }
    }

    fun refreshData() {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val images = getImagesUseCase.getHomeScreenImages()
                _uiState.update { it.copy(
                    availableImages = images.size,
                    isLoading = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    availableImages = 0,
                    isLoading = false,
                    message = "載入圖片失敗: ${e.message}"
                )}
            }
        }
    }

    fun changeHomeScreenNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val images = getImagesUseCase.getHomeScreenImages()
                if (images.isNotEmpty()) {
                    val useRandom = settingsDataStore.settings.first().shuffleEnabled
                    val image = if (useRandom) images.random() else {
                        val currentIndex = settingsDataStore.currentHomeIndex.first()
                        val nextIndex = (currentIndex + 1) % images.size.coerceAtLeast(1)
                        settingsDataStore.updateCurrentHomeIndex(nextIndex)
                        images[nextIndex]
                    }
                    val mode = if (useRandom) "隨機" else "順序"
                    val result = changeWallpaperUseCase.changeHomeScreen(image.uri)
                    _uiState.update { it.copy(
                        message = if (result.isSuccess) "主螢幕桌布已更換($mode)" else "更換失敗",
                        isLoading = false
                    )}
                } else {
                    _uiState.update { it.copy(
                        message = "沒有可用圖片，請先選擇資料夾",
                        isLoading = false
                    )}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    message = "錯誤: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun changeLockScreenNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val images = getImagesUseCase.getLockScreenImages()
                if (images.isNotEmpty()) {
                    val useRandom = settingsDataStore.settings.first().shuffleEnabled
                    val image = if (useRandom) images.random() else {
                        val currentIndex = settingsDataStore.currentLockIndex.first()
                        val nextIndex = (currentIndex + 1) % images.size.coerceAtLeast(1)
                        settingsDataStore.updateCurrentLockIndex(nextIndex)
                        images[nextIndex]
                    }
                    val mode = if (useRandom) "隨機" else "順序"
                    val result = changeWallpaperUseCase.changeLockScreen(image.uri)
                    _uiState.update { it.copy(
                        message = if (result.isSuccess) "鎖屏桌布已更換($mode)" else "更換失敗",
                        isLoading = false
                    )}
                } else {
                    _uiState.update { it.copy(
                        message = "沒有可用圖片，請先選擇資料夾",
                        isLoading = false
                    )}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    message = "錯誤: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun changeBothNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val homeImages = getImagesUseCase.getHomeScreenImages()
                val lockImages = getImagesUseCase.getLockScreenImages()
                if (homeImages.isNotEmpty() && lockImages.isNotEmpty()) {
                    val useRandom = settingsDataStore.settings.first().shuffleEnabled
                    val image = if (useRandom) homeImages.random() else {
                        val currentIndex = settingsDataStore.currentHomeIndex.first()
                        val nextIndex = (currentIndex + 1) % homeImages.size.coerceAtLeast(1)
                        settingsDataStore.updateCurrentHomeIndex(nextIndex)
                        homeImages[nextIndex]
                    }
                    val mode = if (useRandom) "隨機" else "順序"
                    val result = changeWallpaperUseCase.changeBoth(image.uri)
                    _uiState.update { it.copy(
                        message = if (result.isSuccess) "主螢幕和鎖屏桌布已更換($mode)" else "更換失敗",
                        isLoading = false
                    )}
                } else {
                    _uiState.update { it.copy(
                        message = "沒有可用圖片，請先選擇資料夾",
                        isLoading = false
                    )}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    message = "錯誤: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun changeRandomNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val images = getImagesUseCase.getHomeScreenImages()
                if (images.isNotEmpty()) {
                    val image = images.random()
                    val result = changeWallpaperUseCase.changeBoth(image.uri)
                    _uiState.update { it.copy(
                        message = if (result.isSuccess) "隨機桌布已更換" else "更換失敗",
                        isLoading = false
                    )}
                } else {
                    _uiState.update { it.copy(
                        message = "沒有可用圖片，請先選擇資料夾",
                        isLoading = false
                    )}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    message = "錯誤: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun openSlideshow() {
        try {
            val intent = Intent(context, SlideshowActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "無法開啟電子相簿: ${e.message}") }
        }
    }

    fun setShuffle(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateShuffleEnabled(enabled)
            _uiState.update { it.copy(shuffleEnabled = enabled) }
        }
    }

    fun setQuickChangeBubble(enabled: Boolean) {
        if (enabled) {
            // Check if overlay permission is granted
            if (!Settings.canDrawOverlays(context)) {
                // Request overlay permission
                _uiState.update { it.copy(quickChangeBubbleEnabled = false) }
                // Launch settings to request permission
                try {
                    val intent = android.content.Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return
            }
        }
        
        viewModelScope.launch {
            settingsDataStore.updateQuickChangeBubble(enabled)
            _uiState.update { it.copy(quickChangeBubbleEnabled = enabled) }
            
            // Start or stop the floating bubble service
            val intent = android.content.Intent(context, com.autowallpaper.changer.service.FloatingBallService::class.java)
            if (enabled) {
                try {
                    context.startService(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    context.stopService(intent)
                } catch (e: Exception) {
                    // Service might not be running, ignore
                    e.printStackTrace()
                }
            }
        }
    }
}
