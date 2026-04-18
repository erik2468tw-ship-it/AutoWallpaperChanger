package com.autowallpaper.changer.service

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.autowallpaper.changer.BuildConfig
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object ForceUpdateDialog {

    private const val PREFS_NAME = "update_prefs"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * 檢查並顯示強制更新對話框（可能阻擋使用直到更新）
     */
    fun checkAndShow(context: Context) {
        // 先檢查本地是否有阻擋狀態
        if (isBlocked(context)) {
            // 有阻擋狀態，重新檢查線上版本確認是否仍需阻擋
            scope.launch {
                val stillBlocked = checkServerForBlock(context)
                if (stillBlocked) {
                    showForced(context, getBlockedVersion(context), getBlockedUrl(context))
                } else {
                    // 線上版本已降級，清除阻擋狀態
                    clearBlockedState(context)
                }
            }
        }
    }
    
    /**
     * 檢查線上版本是否仍為必要更新
     */
    private suspend fun checkServerForBlock(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentVersion = getVersionCode(context)
                val url = "${BuildConfig.API_BASE_URL}api/version/check?app=autowallpaper&version_code=$currentVersion"
                
                val client = OkHttpClient()
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext false
                    val json = JSONObject(body)
                    
                    // 如果線上版本仍顯示需要更新且為強制，才維持阻擋
                    val hasUpdate = json.optBoolean("has_update", false)
                    val isMandatory = json.optBoolean("is_mandatory", false)
                    
                    if (hasUpdate && isMandatory) {
                        val latestVersion = json.optJSONObject("latest_version")
                        val latestCode = latestVersion?.optInt("version_code", 0) ?: 0
                        // 比對是否是同一個版本（避免線上換成更新的版本但本地還是舊的blocked）
                        val blockedCode = getBlockedVersion(context)
                        return@withContext latestCode == blockedCode
                    }
                    
                    false  // 不再需要阻擋
                } else {
                    false  // 網路錯誤，預設不阻擋
                }
            } catch (e: Exception) {
                false  // 發生錯誤，預設不阻擋避免使用者無法使用
            }
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
    
    private fun getVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }
}