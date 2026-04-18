package com.autowallpaper.changer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.presentation.AutoWallpaperTheme
import com.autowallpaper.changer.presentation.navigation.MainNavigation
import com.autowallpaper.changer.service.AnalyticsService
import com.autowallpaper.changer.service.ForceUpdateDialog
import com.autowallpaper.changer.service.VersionCheckWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results - just check if granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 1. 追蹤 Analytics
        AnalyticsService.trackInstall(this)
        
        // 2. 檢查強制更新（可能阻擋使用直到更新）
        ForceUpdateDialog.checkAndShow(this)
        
        // 3. 啟動定期版本檢查（每 30 分鐘）
        VersionCheckWorker.schedule(this)
        
        // Request permissions on startup (only shows dialog, no navigation)
        requestRequiredPermissions()
        
        setContent {
            val settings by settingsDataStore.settings.collectAsState(initial = null)

            val isDarkTheme = settings?.isDarkTheme ?: false

            AutoWallpaperTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次回到 App 都檢查是否被阻擋
        ForceUpdateDialog.checkAndShow(this)
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Read media images permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Read external storage (Android 12 and below)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
