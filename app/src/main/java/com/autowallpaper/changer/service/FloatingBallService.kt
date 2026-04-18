package com.autowallpaper.changer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.autowallpaper.changer.MainActivity
import com.autowallpaper.changer.R
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.domain.usecase.GetImagesUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class FloatingBallService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_ball_channel"
        private const val NOTIFICATION_ID = 1001
    }

    @Inject
    lateinit var getImagesUseCase: GetImagesUseCase

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private var windowManager: WindowManager? = null
    private var floatingView: ImageView? = null
    private var isBubbleCreated = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var layoutParams: WindowManager.LayoutParams? = null
    
    private var lastX = 0f
    private var lastY = 0f
    private var initialX = 0f
    private var initialY = 0f
    private var isMoving = false

    override fun onCreate() {
        super.onCreate()
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            createNotificationChannel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (!isBubbleCreated) {
                createFloatingBubble()
            }
            
            // 啟動前景服務並顯示通知
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果 startForeground 失敗，至少讓服務繼續運行
        }
        
        return START_STICKY
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "懸浮球服務",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "懸浮球快速換圖服務"
                    setShowBadge(false)
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("懸浮球已啟用")
            .setContentText("點擊懸浮球可快速更換桌布")
            .setSmallIcon(R.drawable.floating_ball_circle)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createFloatingBubble() {
        try {
            floatingView = ImageView(this).apply {
                setImageResource(R.drawable.floating_ball_circle)
                alpha = 0.7f
            }
            
            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dpToPx(10)
                y = resources.displayMetrics.heightPixels / 2 - dpToPx(24)
            }

            floatingView?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = event.rawX
                        initialY = event.rawY
                        lastX = event.rawX
                        lastY = event.rawY
                        isMoving = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - lastX
                        val dy = event.rawY - lastY
                        
                        if (!isMoving && (Math.abs(dx) > dpToPx(5) || Math.abs(dy) > dpToPx(5))) {
                            isMoving = true
                        }
                        
                        if (isMoving) {
                            layoutParams?.let { params ->
                                params.x += dx.toInt()
                                params.y += dy.toInt()
                                windowManager?.updateViewLayout(floatingView, params)
                            }
                        }
                        
                        lastX = event.rawX
                        lastY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
                            changeWallpaperNow()
                        }
                        true
                    }
                    else -> false
                }
            }

            layoutParams?.let { params ->
                windowManager?.addView(floatingView, params)
                isBubbleCreated = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mainHandler.post {
                Toast.makeText(this, "無法顯示懸浮球: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun changeWallpaperNow() {
        serviceScope.launch {
            try {
                val images = getImagesUseCase.getHomeScreenImages()
                if (images.isNotEmpty()) {
                    val image = images.random()
                    setWallpaper(image.uri)
                    mainHandler.post {
                        Toast.makeText(this@FloatingBallService, "桌布已更換", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    mainHandler.post {
                        Toast.makeText(this@FloatingBallService, "沒有可用圖片", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    Toast.makeText(this@FloatingBallService, "更換失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setWallpaper(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return
                try {
                    val wallpaperManager = WallpaperManager.getInstance(this)
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } finally {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (isBubbleCreated && floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingView = null
            isBubbleCreated = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}