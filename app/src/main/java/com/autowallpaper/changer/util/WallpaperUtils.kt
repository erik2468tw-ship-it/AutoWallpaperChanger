package com.autowallpaper.changer.util

import android.app.WallpaperManager
import android.content.Context
import android.os.Build

object WallpaperUtils {
    fun isLockScreenSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    fun getDisplayWidth(context: Context): Int {
        val wm = WallpaperManager.getInstance(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wm.desiredMinimumWidth
        } else {
            1080
        }
    }

    fun getDisplayHeight(context: Context): Int {
        val wm = WallpaperManager.getInstance(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wm.desiredMinimumHeight
        } else {
            1920
        }
    }

    fun hasWallpaperPermission(context: Context): Boolean {
        return WallpaperManager.getInstance(context).isWallpaperSupported
    }

    fun hasLockScreenSupport(context: Context): Boolean {
        val wm = WallpaperManager.getInstance(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wm.isSetWallpaperAllowed
        } else {
            false
        }
    }
}
