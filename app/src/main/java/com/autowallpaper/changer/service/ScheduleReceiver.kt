package com.autowallpaper.changer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.work.*
import kotlinx.coroutines.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Toast.makeText(context, "開機廣播收到", Toast.LENGTH_SHORT).show()
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }
}

class ScheduleReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "ScheduleReceiver: 收到廣播!", Toast.LENGTH_SHORT).show()
        
        when (intent.action) {
            ACTION_WALLPAPER_CHANGE -> {
                Log.i(TAG, "鬧鐘觸發!")
                Toast.makeText(context, "ScheduleReceiver: 鬧鐘觸發！", Toast.LENGTH_SHORT).show()
                // 使用 WorkManager 執行一次
                enqueueWallpaperWork(context)
            }
            ACTION_TRIGGER_NOW -> {
                Log.i(TAG, "立即執行!")
                Toast.makeText(context, "ScheduleReceiver: 立即執行！", Toast.LENGTH_SHORT).show()
                // 使用 WorkManager 立即執行
                enqueueWallpaperWork(context)
            }
        }
    }
    
    private fun enqueueWallpaperWork(context: Context) {
        Toast.makeText(context, "ScheduleReceiver: 準備加入 WorkManager...", Toast.LENGTH_SHORT).show()
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        Toast.makeText(context, "ScheduleReceiver: WorkManager 已加入佇列", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "WallpaperWorker 已加入佇列")
    }
    
    companion object {
        private const val TAG = "ScheduleReceiver"
        const val ACTION_WALLPAPER_CHANGE = "com.autowallpaper.changer.ACTION_WALLPAPER_CHANGE"
        const val ACTION_TRIGGER_NOW = "com.autowallpaper.changer.ACTION_TRIGGER_NOW"
    }
}
