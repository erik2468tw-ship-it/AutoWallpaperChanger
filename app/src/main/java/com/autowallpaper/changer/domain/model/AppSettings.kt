package com.autowallpaper.changer.domain.model

data class AppSettings(
    val isDarkTheme: Boolean = false,
    val wifiOnlyDownload: Boolean = true,
    val lowBatteryPause: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    val autoStartEnabled: Boolean = true,
    val showDaydreamOnCharge: Boolean = true,
    val daydreamClockEnabled: Boolean = true,
    val daydreamWeatherEnabled: Boolean = true,
    val autoClearCache: Boolean = true,
    val cacheMaxDays: Int = 7,
    val selectedHomeFolders: List<String> = emptyList(),
    val selectedLockFolders: List<String> = emptyList(),
    val excludedFolders: List<String> = emptyList(),
    val seasonalTags: List<String> = emptyList(),
    val changeHomeScreen: Boolean = true,
    val changeLockScreen: Boolean = true,
    val scheduleIntervalSeconds: Int = 30,
    val slideshowIntervalSeconds: Int = 5,
    val shuffleEnabled: Boolean = true,
    val quickChangeBubbleEnabled: Boolean = false,
    val debugMode: Boolean = false
)
