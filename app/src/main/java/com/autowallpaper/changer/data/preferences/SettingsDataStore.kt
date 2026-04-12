package com.autowallpaper.changer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.autowallpaper.changer.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val LOW_BATTERY_PAUSE = booleanPreferencesKey("low_battery_pause")
        val LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val DAYDREAM_ON_CHARGE = booleanPreferencesKey("daydream_on_charge")
        val DAYDREAM_CLOCK = booleanPreferencesKey("daydream_clock")
        val DAYDREAM_WEATHER = booleanPreferencesKey("daydream_weather")
        val AUTO_CLEAR_CACHE = booleanPreferencesKey("auto_clear_cache")
        val CACHE_MAX_DAYS = intPreferencesKey("cache_max_days")
        val SELECTED_HOME_FOLDERS = stringSetPreferencesKey("selected_home_folders")
        val SELECTED_LOCK_FOLDERS = stringSetPreferencesKey("selected_lock_folders")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val SEASONAL_TAGS = stringSetPreferencesKey("seasonal_tags")
        val LAST_CHANGE_TIME = longPreferencesKey("last_change_time")
        val CURRENT_HOME_INDEX = intPreferencesKey("current_home_index")
        val CURRENT_LOCK_INDEX = intPreferencesKey("current_lock_index")
        val SCHEDULE_INTERVAL = intPreferencesKey("schedule_interval")
        val SLIDESHOW_INTERVAL = intPreferencesKey("slideshow_interval")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val QUICK_CHANGE_BUBBLE = booleanPreferencesKey("quick_change_bubble")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: false,
            wifiOnlyDownload = prefs[Keys.WIFI_ONLY] ?: true,
            lowBatteryPause = prefs[Keys.LOW_BATTERY_PAUSE] ?: true,
            lowBatteryThreshold = prefs[Keys.LOW_BATTERY_THRESHOLD] ?: 20,
            autoStartEnabled = prefs[Keys.AUTO_START] ?: true,
            showDaydreamOnCharge = prefs[Keys.DAYDREAM_ON_CHARGE] ?: true,
            daydreamClockEnabled = prefs[Keys.DAYDREAM_CLOCK] ?: true,
            daydreamWeatherEnabled = prefs[Keys.DAYDREAM_WEATHER] ?: true,
            autoClearCache = prefs[Keys.AUTO_CLEAR_CACHE] ?: true,
            cacheMaxDays = prefs[Keys.CACHE_MAX_DAYS] ?: 7,
            selectedHomeFolders = prefs[Keys.SELECTED_HOME_FOLDERS]?.toList() ?: emptyList(),
            selectedLockFolders = prefs[Keys.SELECTED_LOCK_FOLDERS]?.toList() ?: emptyList(),
            excludedFolders = prefs[Keys.EXCLUDED_FOLDERS]?.toList() ?: emptyList(),
            seasonalTags = prefs[Keys.SEASONAL_TAGS]?.toList() ?: emptyList(),
            scheduleIntervalMinutes = prefs[Keys.SCHEDULE_INTERVAL] ?: 30,
            slideshowIntervalSeconds = prefs[Keys.SLIDESHOW_INTERVAL] ?: 5,
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: true,
            quickChangeBubbleEnabled = prefs[Keys.QUICK_CHANGE_BUBBLE] ?: false
        )
    }

    val lastChangeTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_CHANGE_TIME] ?: 0L
    }

    val currentHomeIndex: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_HOME_INDEX] ?: 0
    }

    val currentLockIndex: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_LOCK_INDEX] ?: 0
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun updateWifiOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY] = enabled
        }
    }

    suspend fun updateLowBatteryPause(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOW_BATTERY_PAUSE] = enabled
        }
    }

    suspend fun updateLowBatteryThreshold(threshold: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOW_BATTERY_THRESHOLD] = threshold
        }
    }

    suspend fun updateDaydreamOnCharge(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAYDREAM_ON_CHARGE] = enabled
        }
    }

    suspend fun updateDaydreamClock(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAYDREAM_CLOCK] = enabled
        }
    }

    suspend fun updateDaydreamWeather(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAYDREAM_WEATHER] = enabled
        }
    }

    suspend fun updateSelectedHomeFolders(folders: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_HOME_FOLDERS] = folders.toSet()
        }
    }

    suspend fun updateSelectedLockFolders(folders: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_LOCK_FOLDERS] = folders.toSet()
        }
    }

    suspend fun updateExcludedFolders(folders: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EXCLUDED_FOLDERS] = folders.toSet()
        }
    }

    suspend fun updateLastChangeTime(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_CHANGE_TIME] = timestamp
        }
    }

    suspend fun updateCurrentHomeIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_HOME_INDEX] = index
        }
    }

    suspend fun updateCurrentLockIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_LOCK_INDEX] = index
        }
    }

    suspend fun updateAutoClearCache(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CLEAR_CACHE] = enabled
        }
    }

    suspend fun updateCacheMaxDays(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHE_MAX_DAYS] = days
        }
    }

    suspend fun updateScheduleInterval(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCHEDULE_INTERVAL] = minutes
        }
    }

    suspend fun updateSlideshowInterval(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SLIDESHOW_INTERVAL] = seconds
        }
    }

    suspend fun updateShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHUFFLE_ENABLED] = enabled
        }
    }

    suspend fun updateQuickChangeBubble(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.QUICK_CHANGE_BUBBLE] = enabled
        }
    }
}
