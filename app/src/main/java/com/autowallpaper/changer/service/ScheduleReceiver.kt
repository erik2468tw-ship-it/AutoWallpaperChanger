package com.autowallpaper.changer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class ScheduleReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WALLPAPER_CHANGE -> {
                Log.i(TAG, "鬧鐘觸發!")
                
                // 檢查螢幕是否關閉
                if (isScreenOff(context)) {
                    Log.i(TAG, "螢幕已關閉，跳過此次更換，重新排程")
                    // 重新排程下一個鬧鐘
                    rescheduleNextAlarm(context)
                    return
                }
                
                executeWallpaperChange(context)
            }
            ACTION_TRIGGER_NOW -> {
                Log.i(TAG, "立即執行!")
                executeWallpaperChange(context)
            }
        }
    }
    
    private fun isScreenOff(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !powerManager.isScreenOn
    }
    
    private fun rescheduleNextAlarm(context: Context) {
        try {
            val scheduler = WallpaperScheduler(context)
            scheduler.reschedule()
        } catch (e: Exception) {
            Log.e(TAG, "重新排程失敗: ${e.message}")
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