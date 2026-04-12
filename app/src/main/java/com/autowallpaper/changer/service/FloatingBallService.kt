package com.autowallpaper.changer.service

import android.app.Service
import android.app.WallpaperManager
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
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.autowallpaper.changer.R
import com.autowallpaper.changer.data.preferences.SettingsDataStore
import com.autowallpaper.changer.domain.usecase.GetImagesUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class FloatingBallService : Service() {

    @Inject
    lateinit var getImagesUseCase: GetImagesUseCase

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private var windowManager: WindowManager? = null
    private var floatingView: ImageView? = null
    private var isBubbleCreated = false
    private var isServiceReady = false
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
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        isServiceReady = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isBubbleCreated) {
            createFloatingBubble()
        }
        return START_STICKY
    }

    private fun createFloatingBubble() {
        try {
            floatingView = ImageView(this).apply {
                setImageResource(R.drawable.floating_ball_icon)
                alpha = 0.5f  // 50% transparency
            }
            
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
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
            
            mainHandler.post {
                Toast.makeText(this, "懸浮球已啟用", Toast.LENGTH_SHORT).show()
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
        isServiceReady = false
        serviceScope.cancel()
        // Safety check - only remove view if it was actually created
        if (isBubbleCreated && floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                // View might already be removed or never added
                e.printStackTrace()
            }
            floatingView = null
            isBubbleCreated = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
