package com.autowallpaper.changer.presentation.screens.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.service.WallpaperScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val isEnabled: Boolean = false,
    val intervalSeconds: Int = 1800,
    val isCustomInterval: Boolean = false,
    val customIntervalSeconds: Int = 60,
    val changeHomeScreen: Boolean = true,
    val changeLockScreen: Boolean = true,
    val shuffleEnabled: Boolean = true,
    val slideshowIntervalSeconds: Int = 5,
    val isLoading: Boolean = true
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val wallpaperScheduler: WallpaperScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update { it.copy(
                    isEnabled = settings.scheduleIntervalSeconds > 0,
                    changeHomeScreen = settings.changeHomeScreen,
                    changeLockScreen = settings.changeLockScreen,
                    intervalSeconds = settings.scheduleIntervalSeconds,
                    slideshowIntervalSeconds = settings.slideshowIntervalSeconds,
                    isLoading = false
                )}
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }

        if (enabled) {
            val interval = if (_uiState.value.isCustomInterval) {
                _uiState.value.customIntervalSeconds
            } else {
                _uiState.value.intervalSeconds
            }
            // Save to DataStore so HomeScreen can observe the state
            viewModelScope.launch {
                settingsDataStore.updateScheduleInterval(interval)
            }
            // 直接傳秒給 WallpaperScheduler（使用 AlarmManager）
            wallpaperScheduler.startWithSeconds(interval)
        } else {
            // Save 0 to DataStore to indicate disabled
            viewModelScope.launch {
                settingsDataStore.updateScheduleInterval(0)
            }
            wallpaperScheduler.stop()
        }
    }

    fun setInterval(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.updateScheduleInterval(seconds)
        }
        _uiState.update { it.copy(intervalSeconds = seconds) }
        if (_uiState.value.isEnabled) {
            // 直接傳秒給 WallpaperScheduler
            wallpaperScheduler.startWithSeconds(seconds)
        }
    }

    fun setCustomInterval(enabled: Boolean) {
        _uiState.update { it.copy(isCustomInterval = enabled) }
    }

    fun setCustomIntervalSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.updateScheduleInterval(seconds)
        }
        _uiState.update { it.copy(customIntervalSeconds = seconds) }
        if (_uiState.value.isEnabled && _uiState.value.isCustomInterval) {
            // 直接傳秒給 WallpaperScheduler
            wallpaperScheduler.startWithSeconds(seconds)
        }
    }

    fun setChangeHomeScreen(enabled: Boolean) {
        _uiState.update { it.copy(changeHomeScreen = enabled) }
    }

    fun setChangeLockScreen(enabled: Boolean) {
        _uiState.update { it.copy(changeLockScreen = enabled) }
    }

    fun setShuffle(enabled: Boolean) {
        _uiState.update { it.copy(shuffleEnabled = enabled) }
    }

    fun setSlideshowInterval(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.updateSlideshowInterval(seconds)
        }
        _uiState.update { it.copy(slideshowIntervalSeconds = seconds) }
    }
}
