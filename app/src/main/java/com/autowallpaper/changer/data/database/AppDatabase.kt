package com.autowallpaper.changer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WallpaperHistoryEntity::class, ImageCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wallpaperHistoryDao(): WallpaperHistoryDao
    abstract fun imageCacheDao(): ImageCacheDao
}
