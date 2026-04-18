package com.autowallpaper.changer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScheduleReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WALLPAPER_CHANGE -> {
                Log.i(TAG, "鬧鐘觸發!")
                executeWallpaperChange(context)
            }
            ACTION_TRIGGER_NOW -> {
                Log.i(TAG, "立即執行!")
                executeWallpaperChange(context)
            }
        }
    }
    
    private fun executeWallpaperChange(context: Context) {
        try {
            val serviceIntent = Intent(context, WallpaperChangeService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "啟動服務失敗: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "ScheduleReceiver"
        const val ACTION_WALLPAPER_CHANGE = "com.autowallpaper.changer.ACTION_WALLPAPER_CHANGE"
        const val ACTION_TRIGGER_NOW = "com.autowallpaper.changer.ACTION_TRIGGER_NOW"
    }
}
