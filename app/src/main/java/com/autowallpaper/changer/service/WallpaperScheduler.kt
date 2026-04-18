package com.autowallpaper.changer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
    
    private val handler = Handler(Looper.getMainLooper())
    
    private var currentIntervalSeconds: Int = 0
    private var pendingIntent: PendingIntent? = null

    fun start(intervalMinutes: Int) {
        currentIntervalSeconds = intervalMinutes * 60
        scheduleAlarm()
    }
    
    fun startWithSeconds(intervalSeconds: Int) {
        currentIntervalSeconds = intervalSeconds
        scheduleAlarm()
    }
    
    private fun scheduleAlarm() {
        // 取消之前的鬧鐘
        cancel()
        
        showToast("排程：開始設定鬧鐘，間隔 ${currentIntervalSeconds} 秒")
        
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
                    showToast("排程：精確鬧鐘設定成功")
                } else {
                    // 如果不能設定精確鬧鐘，使用一般鬧鐘
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent!!
                    )
                    showToast("排程：一般鬧鐘設定成功")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent!!
                )
                showToast("排程：鬧鐘設定成功")
            }
            Log.i(TAG, "鬧鐘已設定: ${currentIntervalSeconds}秒後觸發")
        } catch (e: Exception) {
            showToast("排程：設定鬧鐘失敗: ${e.message}")
            Log.e(TAG, "設定鬧鐘失敗: ${e.message}")
        }
    }
    
    // 重新安排下一個鬧鐘（遞迴）
    fun reschedule() {
        if (currentIntervalSeconds > 0) {
            showToast("排程：重新安排下一個鬧鐘")
            scheduleAlarm()
            Log.i(TAG, "已重新安排下一個鬧鐘")
        }
    }

    fun stop() {
        cancel()
        currentIntervalSeconds = 0
        showToast("排程：鬧鐘已取消")
        Log.i(TAG, "鬧鐘已取消")
    }
    
    private fun cancel() {
        pendingIntent?.let { pi ->
            alarmManager.cancel(pi)
            pi.cancel()
        }
        pendingIntent = null
    }

    fun isRunning(): Boolean {
        return currentIntervalSeconds > 0 && pendingIntent != null
    }

    // 立即執行一次（用於測試）
    fun runNow() {
        showToast("排程：執行立即更換")
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_TRIGGER_NOW
        }
        context.sendBroadcast(intent)
        Log.i(TAG, "已發送立即執行廣播")
    }
    
    fun getCurrentIntervalSeconds(): Int = currentIntervalSeconds
    
    // 顯示 Toast（在主執行緒）
    private fun showToast(message: String) {
        handler.post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Ignore toast errors
            }
        }
    }
    
    companion object {
        private const val TAG = "WallpaperScheduler"
        private const val REQUEST_CODE = 1001
    }
}
