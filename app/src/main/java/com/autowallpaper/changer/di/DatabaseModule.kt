package com.autowallpaper.changer.di

import android.content.Context
import androidx.room.Room
import com.autowallpaper.changer.data.database.AppDatabase
import com.autowallpaper.changer.data.database.ImageCacheDao
import com.autowallpaper.changer.data.database.WallpaperHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "auto_wallpaper_db"
        ).build()
    }

    @Provides
    fun provideWallpaperHistoryDao(database: AppDatabase): WallpaperHistoryDao {
        return database.wallpaperHistoryDao()
    }

    @Provides
    fun provideImageCacheDao(database: AppDatabase): ImageCacheDao {
        return database.imageCacheDao()
    }
}
