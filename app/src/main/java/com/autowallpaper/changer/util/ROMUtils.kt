package com.autowallpaper.changer.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object ROMUtils {
    enum class ROMType {
        EMUI,      // Huawei
        MIUI,      // Xiaomi
        COLOR_OS,  // OPPO
        REALME_UI, // Realme
        FUNTCH_OS, // Vivo
        ONEUI,     // Samsung
        STOCK      // Stock Android
    }

    fun getROMType(): ROMType {
        return when {
            isMIUI() -> ROMType.MIUI
            isEMUI() -> ROMType.EMUI
            isColorOS() -> ROMType.COLOR_OS
            isRealmeUI() -> ROMType.REALME_UI
            isFuntchOS() -> ROMType.FUNTCH_OS
            isOneUI() -> ROMType.ONEUI
            else -> ROMType.STOCK
        }
    }

    fun isMIUI(): Boolean {
        return !getSystemProperty("ro.miui.ui.version.name").isNullOrEmpty()
    }

    fun isEMUI(): Boolean {
        return !getSystemProperty("ro.build.version.emui").isNullOrEmpty()
    }

    fun isColorOS(): Boolean {
        return !getSystemProperty("ro.build.version.opporom").isNullOrEmpty()
    }

    fun isRealmeUI(): Boolean {
        return !getSystemProperty("ro.build.version.realme").isNullOrEmpty()
    }

    fun isFuntchOS(): Boolean {
        return !getSystemProperty("ro.vivo.os.version").isNullOrEmpty()
    }

    fun isOneUI(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true) &&
               !getSystemProperty("ro.build.version.oneui").isNullOrEmpty()
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }

    fun getAutoStartIntent(context: Context): Intent? {
        return when (getROMType()) {
            ROMType.MIUI -> getMIUIAutoStartIntent()
            ROMType.EMUI -> getEMUIAutoStartIntent()
            ROMType.COLOR_OS -> getColorOSAutoStartIntent()
            ROMType.REALME_UI -> getRealmeUIAutoStartIntent()
            ROMType.FUNTCH_OS -> getFuntchOSAutoStartIntent()
            else -> null
        }
    }

    private fun getMIUIAutoStartIntent(): Intent? {
        return try {
            Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getEMUIAutoStartIntent(): Intent? {
        return try {
            Intent().apply {
                component = android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getColorOSAutoStartIntent(): Intent? {
        return try {
            Intent().apply {
                component = android.content.ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getRealmeUIAutoStartIntent(): Intent? {
        return try {
            Intent().apply {
                component = android.content.ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFuntchOSAutoStartIntent(): Intent? {
        return try {
            Intent().apply {
                component = android.content.ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun openAutoStartSettings(context: Context) {
        getAutoStartIntent(context)?.let { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to app settings
                openAppSettings(context)
            }
        } ?: openAppSettings(context)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
