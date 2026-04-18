package com.autowallpaper.changer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // 從持久化存儲恢覆 interval
    private var currentIntervalSeconds: Int = prefs.getInt(PREF_INTERVAL, 0)
    private var pendingIntent: PendingIntent? = null

    fun start(intervalMinutes: Int) {
        currentIntervalSeconds = intervalMinutes * 60
        saveInterval()
        scheduleAlarm()
    }
    
    fun startWithSeconds(intervalSeconds: Int) {
        currentIntervalSeconds = intervalSeconds
        saveInterval()
        scheduleAlarm()
    }
    
    private fun saveInterval() {
        prefs.edit().putInt(PREF_INTERVAL, currentIntervalSeconds).apply()
    }
    
    private fun scheduleAlarm() {
        // 取消之前的鬧鐘
        cancel()
        
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_WALLPAPER_CHANGE
        }
        
        pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTimeMillis = System.currentTimeMillis() + (currentIntervalSeconds * 1000L)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent!!
                    )
                } else {
                    // 如果不能設定精確鬧鐘，使用一般鬧鐘
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent!!
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent!!
                )
            }
            Log.i(TAG, "鬧鐘已設定: ${currentIntervalSeconds}秒後觸發")
        } catch (e: Exception) {
            Log.e(TAG, "設定鬧鐘失敗: ${e.message}")
        }
    }
    
    // 重新安排下一個鬧鐘（遞迴）
    fun reschedule() {
        if (currentIntervalSeconds > 0) {
            scheduleAlarm()
            Log.i(TAG, "已重新安排下一個鬧鐘")
        } else {
            // 從持久化存儲讀取 interval
            currentIntervalSeconds = prefs.getInt(PREF_INTERVAL, 0)
            if (currentIntervalSeconds > 0) {
                scheduleAlarm()
                Log.i(TAG, "從存儲恢復並重新安排: ${currentIntervalSeconds}秒")
            } else {
                Log.w(TAG, "無法重新安排: 沒有保存的 interval")
            }
        }
    }

    fun stop() {
        cancel()
        currentIntervalSeconds = 0
        clearInterval()
        Log.i(TAG, "鬧鐘已取消")
    }
    
    private fun clearInterval() {
        prefs.edit().remove(PREF_INTERVAL).apply()
    }
    
    private fun cancel() {
        pendingIntent?.let { pi ->
            alarmManager.cancel(pi)
            pi.cancel()
        }
        pendingIntent = null
    }

    fun isRunning(): Boolean {
        // 檢查是否有有效的 interval（從持久化或內存）
        val storedInterval = prefs.getInt(PREF_INTERVAL, 0)
        return (currentIntervalSeconds > 0 || storedInterval > 0) && pendingIntent != null
    }

    // 立即執行一次（用於測試）
    fun runNow() {
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_TRIGGER_NOW
        }
        context.sendBroadcast(intent)
        Log.i(TAG, "已發送立即執行廣播")
    }
    
    fun getCurrentIntervalSeconds(): Int {
        if (currentIntervalSeconds == 0) {
            currentIntervalSeconds = prefs.getInt(PREF_INTERVAL, 0)
        }
        return currentIntervalSeconds
    }
    
    companion object {
        private const val TAG = "WallpaperScheduler"
        private const val REQUEST_CODE = 1001
        private const val PREFS_NAME = "wallpaper_scheduler_prefs"
        private const val PREF_INTERVAL = "schedule_interval_seconds"
    }
}