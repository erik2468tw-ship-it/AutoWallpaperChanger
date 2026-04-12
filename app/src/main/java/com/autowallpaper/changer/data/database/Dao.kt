package com.autowallpaper.changer.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperHistoryDao {
    @Query("SELECT * FROM wallpaper_history ORDER BY usedAt DESC")
    fun getAllHistory(): Flow<List<WallpaperHistoryEntity>>

    @Query("SELECT * FROM wallpaper_history WHERE wallpaperType = :type ORDER BY usedAt DESC LIMIT :limit")
    fun getHistoryByType(type: Int, limit: Int = 50): Flow<List<WallpaperHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: WallpaperHistoryEntity)

    @Query("DELETE FROM wallpaper_history WHERE usedAt < :timestamp")
    suspend fun deleteOldHistory(timestamp: Long)

    @Query("DELETE FROM wallpaper_history")
    suspend fun clearAll()
}

@Dao
interface ImageCacheDao {
    @Query("SELECT * FROM image_cache")
    fun getAllCachedImages(): Flow<List<ImageCacheEntity>>

    @Query("SELECT * FROM image_cache WHERE folderPath = :folderPath")
    fun getCachedImagesByFolder(folderPath: String): Flow<List<ImageCacheEntity>>

    @Query("SELECT * FROM image_cache WHERE uri = :uri")
    suspend fun getCachedImage(uri: String): ImageCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ImageCacheEntity>)

    @Query("DELETE FROM image_cache WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)

    @Query("DELETE FROM image_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM image_cache")
    suspend fun getCacheCount(): Int
}
