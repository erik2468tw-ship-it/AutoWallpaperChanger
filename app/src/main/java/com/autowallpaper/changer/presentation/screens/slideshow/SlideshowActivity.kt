package com.autowallpaper.changer.presentation.screens.slideshow

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.autowallpaper.changer.data.local.ImageScanner
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.domain.model.WallpaperItem
import com.autowallpaper.changer.presentation.AutoWallpaperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class SlideshowActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on during slideshow
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val imageScanner = ImageScanner(this)
        val settings = runBlocking { settingsDataStore.settings.first() }
        val folderUris = settings.selectedHomeFolders
        
        val images = mutableListOf<WallpaperItem>()
        for (folderUriString in folderUris) {
            try {
                val folderUri = android.net.Uri.parse(folderUriString)
                val folderImages = runBlocking { imageScanner.scanSafFolder(folderUri) }
                images.addAll(folderImages)
            } catch (e: Exception) {
                // Skip invalid URIs
            }
        }

        val intervalSeconds = settings.slideshowIntervalSeconds.coerceIn(1, 3600)

        setContent {
            AutoWallpaperTheme(darkTheme = settings.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    SlideshowScreen(
                        images = images.shuffled(),
                        intervalSeconds = intervalSeconds,
                        onExit = { finish() }
                    )
                }
            }
        }
    }
}
