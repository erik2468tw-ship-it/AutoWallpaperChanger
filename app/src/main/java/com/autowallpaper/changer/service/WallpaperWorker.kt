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
    private val wallpaperHistoryDao: WallpaperHistoryDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            changeWallpaper()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun changeWallpaper() {
        val settings = settingsDataStore.settings.first()

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

        if (homeImages.isEmpty() && lockImages.isEmpty()) {
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
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return
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
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES,
                1, TimeUnit.MINUTES  // flex interval
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
