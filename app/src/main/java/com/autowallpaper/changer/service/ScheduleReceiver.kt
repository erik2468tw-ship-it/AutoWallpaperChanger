package com.autowallpaper.changer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule wallpaper changes after boot
            // Note: The app needs to be started once to restore the schedule
            // This receiver just triggers the app to start
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
                // This would trigger immediate wallpaper change
                // For now, the WallpaperScheduler handles everything
            }
        }
    }

    companion object {
        const val ACTION_WALLPAPER_CHANGE = "com.autowallpaper.changer.ACTION_WALLPAPER_CHANGE"
    }
}
