package com.autowallpaper.changer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.bootDataStore by preferencesDataStore(name = "app_settings")

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "裝置開機完成")
            
            val pendingResult = goAsync()
            Thread {
                try {
                    val prefs = runBlocking {
                        context.bootDataStore.data.first()
                    }
                    
                    // 檢查排程是否啟用
                    val scheduleInterval = prefs[intPreferencesKey("schedule_interval")] ?: 0
                    if (scheduleInterval > 0) {
                        Log.i(TAG, "恢復排程: $scheduleInterval 秒")
                        val scheduler = WallpaperScheduler(context)
                        scheduler.startWithSeconds(scheduleInterval)
                    }
                    
                    // 檢查懸浮球是否啟用
                    val bubbleEnabled = prefs[booleanPreferencesKey("quick_change_bubble")] ?: false
                    if (bubbleEnabled) {
                        Log.i(TAG, "恢復懸浮球")
                        try {
                            val bubbleIntent = Intent(context, FloatingBallService::class.java)
                            context.startService(bubbleIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "恢復懸浮球失敗: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "開機還原失敗: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }.start()
        }
    }
    
    companion object {
        private const val TAG = "BootReceiver"
    }
}