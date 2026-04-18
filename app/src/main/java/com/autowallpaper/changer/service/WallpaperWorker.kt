package com.autowallpaper.changer.service

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.autowallpaper.changer.data.database.WallpaperHistoryDao
import com.autowallpaper.changer.data.database.WallpaperHistoryEntity
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.domain.usecase.GetImagesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class WallpaperWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getImagesUseCase: GetImagesUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val wallpaperHistoryDao: WallpaperHistoryDao,
    private val wallpaperScheduler: WallpaperScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        android.util.Log.i("WallpaperWorker", "=== doWork() START ===")
        
        return try {
            android.util.Log.i("WallpaperWorker", "Calling changeWallpaper()...")
            changeWallpaper()
            android.util.Log.i("WallpaperWorker", "changeWallpaper() completed successfully")
            
            // 重新安排下一個鬧鐘（遞迴）
            val intervalSeconds = settingsDataStore.settings.first().scheduleIntervalSeconds
            if (intervalSeconds > 0) {
                android.util.Log.i("WallpaperWorker", "Rescheduling alarm for $intervalSeconds seconds")
                wallpaperScheduler.startWithSeconds(intervalSeconds)
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("WallpaperWorker", "Exception in doWork(): ${e.message}")
            e.printStackTrace()
            
            // 即使失敗也重新安排，以後再重試
            val intervalSeconds = settingsDataStore.settings.first().scheduleIntervalSeconds
            if (intervalSeconds > 0) {
                wallpaperScheduler.startWithSeconds(intervalSeconds)
            }
            
            Result.retry()
        }
    }

    private suspend fun changeWallpaper() {
        android.util.Log.i("WallpaperWorker", "===== changeWallpaper() called =====")
        
        val settings = settingsDataStore.settings.first()
        android.util.Log.i("WallpaperWorker", "Settings: scheduleIntervalSeconds=${settings.scheduleIntervalSeconds}, selectedFolders=${settings.selectedHomeFolders.size}")

        // Check low battery condition
        if (settings.lowBatteryPause) {
            try {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (batteryLevel < settings.lowBatteryThreshold) {
                    return
                }
            } catch (e: Exception) {
                // Ignore battery check errors
            }
        }

        // Get images
        val homeImages = getImagesUseCase.getHomeScreenImages()
        val lockImages = getImagesUseCase.getLockScreenImages()
        android.util.Log.i("WallpaperWorker", "Found images: home=${homeImages.size}, lock=${lockImages.size}")

        if (homeImages.isEmpty() && lockImages.isEmpty()) {
            android.util.Log.w("WallpaperWorker", "No images found, returning early")
            return
        }

        // Get current indices
        val currentHomeIndex = settingsDataStore.currentHomeIndex.first()
        val currentLockIndex = settingsDataStore.currentLockIndex.first()
        val wallpaperManager = WallpaperManager.getInstance(context)

        // Change home screen
        if (settings.changeHomeScreen && homeImages.isNotEmpty()) {
            val nextHomeIndex = (currentHomeIndex + 1) % homeImages.size.coerceAtLeast(1)
            if (nextHomeIndex < homeImages.size) {
                val image = homeImages[nextHomeIndex]
                setHomeScreenWallpaper(image.uri, wallpaperManager)
                settingsDataStore.updateCurrentHomeIndex(nextHomeIndex)
            }
        }

        // Change lock screen
        if (settings.changeLockScreen && lockImages.isNotEmpty()) {
            val nextLockIndex = (currentLockIndex + 1) % lockImages.size.coerceAtLeast(1)
            if (nextLockIndex < lockImages.size) {
                val image = lockImages[nextLockIndex]
                setLockScreenWallpaper(image.uri, wallpaperManager)
                settingsDataStore.updateCurrentLockIndex(nextLockIndex)
            }
        }
    }

    private suspend fun setHomeScreenWallpaper(uri: android.net.Uri, wallpaperManager: WallpaperManager) {
        android.util.Log.i("WallpaperWorker", "Setting home wallpaper: $uri")
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: run {
                    android.util.Log.e("WallpaperWorker", "Failed to decode bitmap")
                    return
                }
                try {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    try {
                        wallpaperHistoryDao.insert(
                            WallpaperHistoryEntity(
                                imageUri = uri.toString(),
                                wallpaperType = 0,
                                usedAt = System.currentTimeMillis(),
                                scheduleId = 0
                            )
                        )
                    } catch (e: Exception) {
                        // Ignore DB errors
                    }
                    try {
                        settingsDataStore.updateLastChangeTime(System.currentTimeMillis())
                    } catch (e: Exception) {
                        // Ignore settings errors
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun setLockScreenWallpaper(uri: android.net.Uri, wallpaperManager: WallpaperManager) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return
                try {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    try {
                        wallpaperHistoryDao.insert(
                            WallpaperHistoryEntity(
                                imageUri = uri.toString(),
                                wallpaperType = 1,
                                usedAt = System.currentTimeMillis(),
                                scheduleId = 0
                            )
                        )
                    } catch (e: Exception) {
                        // Ignore DB errors
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val WORK_NAME = "wallpaper_change_work"

        fun schedule(context: Context, intervalMinutes: Int) {
            android.util.Log.i("WallpaperWorker", "schedule() called with intervalMinutes=$intervalMinutes")
            
            // WorkManager 最小間隔是 15 分鐘
            val safeInterval = maxOf(intervalMinutes.toLong(), 15L)
            
            // 移除所有限制，確保一定會執行
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
                safeInterval, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES  // flex interval
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,  // 改為 REPLACE 確保每次都替換
                    workRequest
                )
            
            android.util.Log.i("WallpaperWorker", "WorkManager scheduled successfully")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            android.util.Log.i("WallpaperWorker", "WorkManager cancelled")
        }
        
        // 立即執行一次排程（用於測試）
        fun runNow(context: Context) {
            android.util.Log.i("WallpaperWorker", "runNow() called")
            
            // 創建 OneTimeWorkRequest
            val runRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                .build()
            
            android.util.Log.i("WallpaperWorker", "Enqueuing OneTimeWorkRequest")
            
            // 使用 applicationContext 確保 WorkManager 可以正常工作
            WorkManager.getInstance(context.applicationContext).enqueue(runRequest)
            
            android.util.Log.i("WallpaperWorker", "OneTimeWorkRequest enqueued successfully")
        }
    }
}
