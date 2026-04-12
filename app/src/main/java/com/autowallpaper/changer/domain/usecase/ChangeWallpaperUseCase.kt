package com.autowallpaper.changer.domain.usecase

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.autowallpaper.changer.data.database.WallpaperHistoryDao
import com.autowallpaper.changer.data.database.WallpaperHistoryEntity
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class ChangeWallpaperUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val wallpaperHistoryDao: WallpaperHistoryDao
) {
    private val wallpaperManager = WallpaperManager.getInstance(context)

    suspend fun changeHomeScreen(imageUri: android.net.Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmap(imageUri) ?: return@withContext Result.failure(
                IOException("Failed to load image")
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }

                try {
                    wallpaperHistoryDao.insert(
                        WallpaperHistoryEntity(
                            imageUri = imageUri.toString(),
                            wallpaperType = 0,
                            usedAt = System.currentTimeMillis(),
                            scheduleId = 0
                        )
                    )
                } catch (e: Exception) {
                    // Ignore DB error
                }

                try {
                    settingsDataStore.updateLastChangeTime(System.currentTimeMillis())
                } catch (e: Exception) {
                    // Ignore settings error
                }
            } finally {
                bitmap.recycle()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun changeLockScreen(imageUri: android.net.Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return@withContext Result.failure(
                    IOException("Lock screen wallpaper not supported on this Android version")
                )
            }

            val bitmap = loadBitmap(imageUri) ?: return@withContext Result.failure(
                IOException("Failed to load image")
            )

            try {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)

                try {
                    wallpaperHistoryDao.insert(
                        WallpaperHistoryEntity(
                            imageUri = imageUri.toString(),
                            wallpaperType = 1,
                            usedAt = System.currentTimeMillis(),
                            scheduleId = 0
                        )
                    )
                } catch (e: Exception) {
                    // Ignore DB error
                }
            } finally {
                bitmap.recycle()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun changeBoth(imageUri: android.net.Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmap(imageUri) ?: return@withContext Result.failure(
                IOException("Failed to load image")
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }

                try {
                    wallpaperHistoryDao.insert(
                        WallpaperHistoryEntity(
                            imageUri = imageUri.toString(),
                            wallpaperType = 2,
                            usedAt = System.currentTimeMillis(),
                            scheduleId = 0
                        )
                    )
                } catch (e: Exception) {
                    // Ignore DB error
                }

                try {
                    settingsDataStore.updateLastChangeTime(System.currentTimeMillis())
                } catch (e: Exception) {
                    // Ignore settings error
                }
            } finally {
                bitmap.recycle()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun loadBitmap(uri: android.net.Uri): Bitmap? {
        return try {
            // Limit sample size to avoid OOM
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Start with 2x downscale
                inJustDecodeBounds = false
            }
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    suspend fun isWallpaperSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
}
