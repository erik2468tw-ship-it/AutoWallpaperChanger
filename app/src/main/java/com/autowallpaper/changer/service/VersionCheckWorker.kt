package com.autowallpaper.changer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.autowallpaper.changer.BuildConfig
import com.autowallpaper.changer.MainActivity
import com.autowallpaper.changer.R
import com.google.gson.annotations.SerializedName
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@HiltWorker
class VersionCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "version_check_work"
        private const val TAG = "VersionChecker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<VersionCheckWorker>(
                30, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun checkNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<VersionCheckWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME + "_immediate", ExistingWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val updateInfo = checkForUpdate()

            if (updateInfo != null && updateInfo.hasUpdate) {
                showUpdateNotification(updateInfo)
                if (updateInfo.isMandatory) {
                    saveBlockedState(updateInfo)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkForUpdate(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val currentVersion = getVersionCode()
                val response = api.checkUpdate(currentVersion).execute()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.hasUpdate == true) {
                        UpdateInfo(
                            hasUpdate = true,
                            version = body.latestVersion?.version ?: "",
                            versionCode = body.latestVersion?.versionCode ?: 0,
                            releaseNotes = body.latestVersion?.releaseNotes ?: "",
                            downloadUrl = body.latestVersion?.downloadUrl ?: "",
                            isMandatory = body.isMandatory == true
                        )
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun showUpdateNotification(updateInfo: UpdateInfo) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

        // 避免重複通知
        val lastNotifiedVersion = prefs.getInt("last_notified_version", 0)
        if (lastNotifiedVersion == updateInfo.versionCode) return

        val channelId = if (updateInfo.isMandatory) "mandatory_update" else "update"
        createNotificationChannel(channelId, updateInfo.isMandatory)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "update")
            putExtra("download_url", updateInfo.downloadUrl)
            putExtra("version_code", updateInfo.versionCode)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_update)  // 需要替換為您現有的 icon
            .setContentTitle(if (updateInfo.isMandatory) "⚠️ 必須更新" else "🌟 發現新版本")
            .setContentText("版本 ${updateInfo.version} 已發布")
            .setStyle(NotificationCompat.BigTextStyle().bigText(updateInfo.releaseNotes))
            .setPriority(if (updateInfo.isMandatory) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (updateInfo.isMandatory) {
            builder.setOngoing(true)
                .addAction(0, "立即更新", pendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(channelId, updateInfo.versionCode, builder.build())

        prefs.edit().putInt("last_notified_version", updateInfo.versionCode).apply()
    }

    private fun createNotificationChannel(channelId: String, isMandatory: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (isMandatory) "必須更新" else "版本更新"
            val importance = if (isMandatory) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "版本更新通知"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveBlockedState(updateInfo: UpdateInfo) {
        context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_blocked", true)
            .putInt("blocked_version", updateInfo.versionCode)
            .putString("blocked_url", updateInfo.downloadUrl)
            .apply()
    }

    private fun getVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                pInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    private val api: UpdateApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateApi::class.java)
    }
}

interface UpdateApi {
    @GET("api/version/check")
    fun checkUpdate(@Query("version_code") versionCode: Int): retrofit2.Call<VersionCheckResponse>
}

data class VersionCheckResponse(
    @SerializedName("has_update") val hasUpdate: Boolean,
    @SerializedName("is_mandatory") val isMandatory: Boolean?,
    @SerializedName("latest_version") val latestVersion: LatestVersion?
)

data class LatestVersion(
    @SerializedName("version") val version: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("min_version_code") val minVersionCode: Int?,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("release_notes") val releaseNotes: String
)

data class UpdateInfo(
    val hasUpdate: Boolean,
    val version: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val isMandatory: Boolean
)
