package com.autowallpaper.changer.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpaper_history")
data class WallpaperHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val wallpaperType: Int, // 0 = home, 1 = lock, 2 = both
    val usedAt: Long,
    val scheduleId: Long
)

@Entity(tableName = "image_cache")
data class ImageCacheEntity(
    @PrimaryKey
    val uri: String,
    val displayName: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val dateTaken: Long,
    val folderPath: String,
    val tags: String, // JSON array as string
    val cachedAt: Long
)
