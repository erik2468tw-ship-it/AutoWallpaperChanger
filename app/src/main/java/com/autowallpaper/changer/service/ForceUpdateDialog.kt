package com.autowallpaper.changer.service

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ForceUpdateDialog {

    private const val PREFS_NAME = "update_prefs"

    /**
     * 檢查並顯示強制更新對話框（如果被阻擋）
     */
    fun checkAndShow(context: Context) {
        if (isBlocked(context)) {
            showForced(context, getBlockedVersion(context), getBlockedUrl(context))
        }
    }

    /**
     * 顯示強制更新對話框（無法取消）
     */
    fun showForced(context: Context, versionCode: Int, downloadUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("⚠️ 必須更新")
            .setMessage("您的版本已過舊，無法繼續使用。\n\n請更新到最新版本後才能繼續使用 App。")
            .setPositiveButton("立即更新") { _, _ ->
                downloadAndInstall(context, downloadUrl)
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 下載並安裝
     */
    private fun downloadAndInstall(context: Context, downloadUrl: String) {
        if (downloadUrl.isBlank()) {
            Toast.makeText(context, "正在打開下載頁面...", Toast.LENGTH_SHORT).show()
            try {
                val intent = Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "請手動更新 App", Toast.LENGTH_LONG).show()
            }
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        context.startActivity(intent)
    }

    // ==================== Block State ====================

    private fun isBlocked(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("is_blocked", false)
    }

    fun clearBlockedState(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_blocked", false)
            .apply()
    }

    private fun getBlockedVersion(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("blocked_version", 0)
    }

    private fun getBlockedUrl(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("blocked_url", "") ?: ""
    }
}
