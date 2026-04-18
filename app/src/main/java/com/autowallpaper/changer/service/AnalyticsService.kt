package com.autowallpaper.changer.service

import android.content.Context
import android.os.Build
import android.util.Log
import com.autowallpaper.changer.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Analytics Service - 追蹤安裝和使用統計
 * 不使用 Firebase，純 HTTP 上報
 */
object AnalyticsService {

    private const val TAG = "Analytics"
    private const val PREFS_NAME = "analytics_prefs"
    private const val KEY_ANONYMOUS_ID = "anonymous_id"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /**
     * 追蹤事件
     */
    fun trackEvent(context: Context, event: String, params: Map<String, String> = emptyMap()) {
        scope.launch {
            try {
                val anonymousId = getAnonymousId(context)
                val body = mapOf(
                    "anonymous_id" to anonymousId,
                    "app_version" to getAppVersion(context),
                    "os_version" to "Android ${Build.VERSION.SDK_INT}",
                    "event" to event,
                    "params" to params
                )

                val json = gson.toJson(body)
                val requestBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}api/track")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Tracked: $event")
                    } else {
                        Log.w(TAG, "Track failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Track error", e)
            }
        }
    }

    /**
     * 追蹤 App 安裝/活躍
     */
    fun trackInstall(context: Context) {
        trackEvent(context, "app_open", mapOf("timestamp" to System.currentTimeMillis().toString()))
    }

    /**
     * 追蹤圖庫使用
     */
    fun trackGalleryView(context: Context, category: String?) {
        trackEvent(context, "gallery_view", mapOf("category" to (category ?: "all")))
    }

    /**
     * 追蹤桌布設定
     */
    fun trackWallpaperSet(context: Context, source: String) {
        trackEvent(context, "wallpaper_set", mapOf("source" to source))
    }

    /**
     * 取得匿名 ID
     */
    private fun getAnonymousId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ANONYMOUS_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_ANONYMOUS_ID, newId).apply()
            newId
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
