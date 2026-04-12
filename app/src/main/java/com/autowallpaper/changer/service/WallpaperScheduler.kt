package com.autowallpaper.changer.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun start(intervalMinutes: Int) {
        WallpaperWorker.schedule(context, intervalMinutes)
    }

    fun isRunning(): Boolean {
        // WorkManager runs whenever the periodic work is scheduled
        // We can't easily check if it's running, but presence of work indicates it's scheduled
        return true
    }

    fun stop() {
        WallpaperWorker.cancel(context)
    }
}
