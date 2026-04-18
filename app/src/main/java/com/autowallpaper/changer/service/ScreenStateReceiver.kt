package com.autowallpaper.changer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.screenDataStore by preferencesDataStore(name = "app_settings")

class ScreenStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ScreenStateReceiver"
        private const val PREFS_NAME = "wallpaper_scheduler_prefs"
        private const val PREF_SCHEDULER_PAUSED = "scheduler_paused"
        private const val PREF_SCHEDULER_INTERVAL = "schedule_interval_seconds"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.i(TAG, "螢幕關閉")
                pauseScheduler(context)
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.i(TAG, "螢幕開啟")
                resumeScheduler(context)
            }
        }
    }
    
    private fun pauseScheduler(context: Context) {
        try {
            // 保存當前狀態，標記為暫停
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val interval = prefs.getInt(PREF_SCHEDULER_INTERVAL, 0)
            
            if (interval > 0) {
                // 取消當前鬧鐘
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    1001,
                    Intent(context, ScheduleReceiver::class.java).apply {
                        action = ScheduleReceiver.ACTION_WALLPAPER_CHANGE
                    },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                
                // 保存狀態
                prefs.edit()
                    .putBoolean(PREF_SCHEDULER_PAUSED, true)
                    .putInt(PREF_SCHEDULER_INTERVAL, interval)
                    .apply()
                
                Log.i(TAG, "排程已暫停，interval=$interval")
            }
        } catch (e: Exception) {
            Log.e(TAG, "暫停排程失敗: ${e.message}")
        }
    }
    
    private fun resumeScheduler(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val wasPaused = prefs.getBoolean(PREF_SCHEDULER_PAUSED, false)
            val interval = prefs.getInt(PREF_SCHEDULER_INTERVAL, 0)
            
            if (wasPaused && interval > 0) {
                // 恢復排程
                val scheduler = WallpaperScheduler(context)
                scheduler.startWithSeconds(interval)
                
                // 清除暫停標記
                prefs.edit()
                    .putBoolean(PREF_SCHEDULER_PAUSED, false)
                    .apply()
                
                Log.i(TAG, "排程已恢復，interval=$interval")
            }
        } catch (e: Exception) {
            Log.e(TAG, "恢復排程失敗: ${e.message}")
        }
    }
}