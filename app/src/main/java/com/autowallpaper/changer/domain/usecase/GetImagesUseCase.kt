package com.autowallpaper.changer.domain.usecase

import android.net.Uri
import com.autowallpaper.changer.data.local.ImageScanner
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.domain.model.WallpaperItem
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetImagesUseCase @Inject constructor(
    private val imageScanner: ImageScanner,
    private val settingsDataStore: SettingsDataStore
) {
    /**
     * Get all wallpaper images (shared for both home and lock screen)
     */
    suspend fun getWallpaperImages(): List<WallpaperItem> {
        val settings = settingsDataStore.settings.first()
        
        // If no SAF folders selected, return empty list (don't fall back to all device images)
        if (settings.selectedHomeFolders.isEmpty()) {
            return emptyList()
        }
        
        val allImages = mutableListOf<WallpaperItem>()

        // Scan SAF folders
        settings.selectedHomeFolders.forEach { folderUriString ->
            try {
                val folderUri = Uri.parse(folderUriString)
                val folderImages = imageScanner.scanSafFolder(folderUri)
                allImages.addAll(folderImages)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Apply exclusions and seasonal filters
        return allImages
            .distinctBy { it.uri.toString() }
            .filter { image ->
                settings.excludedFolders.none { folder ->
                    image.folderPath.contains(folder)
                }
            }
            .filter { image ->
                settings.seasonalTags.isEmpty() ||
                settings.seasonalTags.any { tag -> image.tags.contains(tag) }
            }
            .sortedByDescending { it.dateTaken }
    }

    /**
     * Get home screen images (uses shared folder)
     */
    suspend fun getHomeScreenImages(): List<WallpaperItem> {
        return getWallpaperImages()
    }

    /**
     * Get lock screen images (uses shared folder - simplified)
     */
    suspend fun getLockScreenImages(): List<WallpaperItem> {
        return getWallpaperImages()
    }

    /**
     * Get all images (unfiltered)
     */
    suspend fun getAllImages(): List<WallpaperItem> {
        return imageScanner.scanAllImages()
    }
}
