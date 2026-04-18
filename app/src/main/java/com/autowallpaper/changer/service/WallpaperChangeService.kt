package com.autowallpaper.changer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.domain.usecase.ChangeWallpaperUseCase
import com.autowallpaper.changer.domain.usecase.GetImagesUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperChangeService : Service() {

    @Inject
    lateinit var getImagesUseCase: GetImagesUseCase

    @Inject
    lateinit var changeWallpaperUseCase: ChangeWallpaperUseCase

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var wallpaperScheduler: WallpaperScheduler

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            try {
                doWork()
            } finally {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }

    private suspend fun doWork() {
        val settings = settingsDataStore.settings.first()
        
        // 檢查低電量
        if (settings.lowBatteryPause) {
            val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (batteryLevel < settings.lowBatteryThreshold) {
                return
            }
        }
        
        // 取得圖片
        val homeImages = getImagesUseCase.getHomeScreenImages()
        val lockImages = getImagesUseCase.getLockScreenImages()
        
        if (homeImages.isEmpty() && lockImages.isEmpty()) {
            return
        }
        
        // 根據 shuffleEnabled 決定選擇方式
        val useRandom = settings.shuffleEnabled
        
        // 更換主螢幕
        if (settings.changeHomeScreen && homeImages.isNotEmpty()) {
            val image = if (useRandom) {
                homeImages.random()
            } else {
                val currentIndex = settingsDataStore.currentHomeIndex.first()
                val nextIndex = (currentIndex + 1) % homeImages.size.coerceAtLeast(1)
                settingsDataStore.updateCurrentHomeIndex(nextIndex)
                homeImages[nextIndex]
            }
            
            changeWallpaperUseCase.changeHomeScreen(image.uri)
        }
        
        // 更換鎖屏
        if (settings.changeLockScreen && lockImages.isNotEmpty()) {
            val image = if (useRandom) {
                lockImages.random()
            } else {
                val currentIndex = settingsDataStore.currentLockIndex.first()
                val nextIndex = (currentIndex + 1) % lockImages.size.coerceAtLeast(1)
                settingsDataStore.updateCurrentLockIndex(nextIndex)
                lockImages[nextIndex]
            }
            
            changeWallpaperUseCase.changeLockScreen(image.uri)
        }
        
        // 重新安排下一個鬧鐘
        if (settings.scheduleIntervalSeconds > 0) {
            wallpaperScheduler.startWithSeconds(settings.scheduleIntervalSeconds)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
