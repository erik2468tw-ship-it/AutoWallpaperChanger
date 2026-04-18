package com.autowallpaper.changer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }
}

class ScheduleReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WALLPAPER_CHANGE -> {
                Log.i(TAG, "鬧鐘觸發!")
                // 使用 WorkManager 執行一次
                enqueueWallpaperWork(context)
            }
            ACTION_TRIGGER_NOW -> {
                Log.i(TAG, "立即執行!")
                // 使用 WorkManager 立即執行
                enqueueWallpaperWork(context)
            }
        }
    }
    
    private fun enqueueWallpaperWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        Log.i(TAG, "WallpaperWorker 已加入佇列")
    }
    
    companion object {
        private const val TAG = "ScheduleReceiver"
        const val ACTION_WALLPAPER_CHANGE = "com.autowallpaper.changer.ACTION_WALLPAPER_CHANGE"
        const val ACTION_TRIGGER_NOW = "com.autowallpaper.changer.ACTION_TRIGGER_NOW"
    }
}
