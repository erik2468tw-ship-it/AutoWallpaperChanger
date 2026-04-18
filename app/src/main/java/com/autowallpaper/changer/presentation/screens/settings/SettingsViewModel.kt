package com.autowallpaper.changer.presentation.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val wifiOnlyDownload: Boolean = true,
    val lowBatteryPause: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    val autoClearCache: Boolean = true,
    val cacheMaxDays: Int = 7,
    val appVersion: String = "1.0.0",
    val debugMode: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadAppVersion()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update { it.copy(
                    isDarkTheme = settings.isDarkTheme,
                    wifiOnlyDownload = settings.wifiOnlyDownload,
                    lowBatteryPause = settings.lowBatteryPause,
                    lowBatteryThreshold = settings.lowBatteryThreshold,
                    autoClearCache = settings.autoClearCache,
                    cacheMaxDays = settings.cacheMaxDays,
                    debugMode = settings.debugMode
                )}
            }
        }
    }

    private fun loadAppVersion() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            _uiState.update { it.copy(appVersion = packageInfo.versionName ?: "1.0.0") }
        } catch (e: Exception) {
            // Use default
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDarkTheme(enabled)
            _uiState.update { it.copy(isDarkTheme = enabled) }
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateWifiOnly(enabled)
            _uiState.update { it.copy(wifiOnlyDownload = enabled) }
        }
    }

    fun setLowBatteryPause(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateLowBatteryPause(enabled)
            _uiState.update { it.copy(lowBatteryPause = enabled) }
        }
    }

    fun setLowBatteryThreshold(threshold: Int) {
        viewModelScope.launch {
            settingsDataStore.updateLowBatteryThreshold(threshold)
            _uiState.update { it.copy(lowBatteryThreshold = threshold) }
        }
    }

    fun setAutoClearCache(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoClearCache(enabled)
            _uiState.update { it.copy(autoClearCache = enabled) }
        }
    }

    fun setCacheMaxDays(days: Int) {
        viewModelScope.launch {
            settingsDataStore.updateCacheMaxDays(days)
            _uiState.update { it.copy(cacheMaxDays = days) }
        }
    }
    
    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDebugMode(enabled)
            _uiState.update { it.copy(debugMode = enabled) }
        }
    }
}
