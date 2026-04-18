package com.autowallpaper.changer.service

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.contentValuesOf
import com.autowallpaper.changer.BuildConfig
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 線上圖庫 API Service
 */
object GalleryApiService {

    private const val TAG = "GalleryApiService"
    
    private val api: GalleryApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GalleryApi::class.java)
    }

    /**
     * 獲取圖庫列表
     */
    suspend fun getImages(
        category: String? = null,
        page: Int = 1,
        limit: Int = 500
    ): Result<GalleryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getImages(category, page, limit).execute()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 檢查圖片是否已經下載過
     */
    fun isImageAlreadyDownloaded(context: Context, filename: String): Boolean {
        val projection = arrayOf(
            android.provider.MediaStore.Images.Media._ID,
            android.provider.MediaStore.Images.Media.DISPLAY_NAME
        )
        val selection = "${android.provider.MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${android.provider.MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("%Pictures/AutoWallpaper%", filename)
        
        return try {
            val resolver = context.contentResolver
            val query = resolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            val exists = query?.use { cursor -> cursor.count > 0 } ?: false
            Log.d(TAG, "isImageAlreadyDownloaded($filename): $exists")
            exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if image exists", e)
            false
        }
    }
    
    /**
     * 圖片已存在例外
     */
    class ImageAlreadyExistsException : Exception("Image already downloaded")
    
    /**
     * 獲取分類列表
     */
    suspend fun getCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCategories().execute()
            if (response.isSuccessful) {
                Result.success(response.body()!!.categories)
            } else {
                Result.failure(Exception("API error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 下載圖片到本地
     */
    suspend fun downloadImage(context: Context, imageUrl: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = if (imageUrl.startsWith("http")) imageUrl else "${BuildConfig.API_BASE_URL.removeSuffix("/")}$imageUrl"
            val request = Request.Builder().url(url).build()
            
            val response = api.downloadImage(url).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed"))
            }
            
            val cacheDir = File(context.cacheDir, "gallery_downloads")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val file = File(cacheDir, "wallpaper_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                response.body()!!.byteStream().copyTo(output)
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    /**
     * 下載圖片到專用資料夾（AutoWallpaper）
     * 回傳下載後的 Uri
     */
    suspend fun downloadToGallery(context: Context, imageUrl: String, filename: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // 先檢查是否已經下載過
            if (isImageAlreadyDownloaded(context, filename)) {
                return@withContext Result.failure(ImageAlreadyExistsException())
            }
            
            val url = if (imageUrl.startsWith("http")) imageUrl else "${BuildConfig.API_BASE_URL.removeSuffix("/")}$imageUrl"
            
            val request = Request.Builder().url(url).build()
            val response = api.downloadImage(url).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed"))
            }
            
            val bytes = response.body()!!.bytes()
            
            // 使用 MediaStore 儲存到Pictures/AutoWallpaper
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AutoWallpaper")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
            
            val resolver = context.contentResolver
            val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))
            
            resolver.openOutputStream(imageUri)?.use { output ->
                output.write(bytes)
            }
            
            // 標記下載完成
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
            
            Log.d(TAG, "Downloaded to gallery: $imageUri")
            Result.success(imageUri)
        } catch (e: Exception) {
            Log.e(TAG, "Download to gallery failed", e)
            Result.failure(e)
        }
    }

    /**
     * 下載並設為桌布
     */
    suspend fun downloadAndSetWallpaper(
        context: Context,
        imageUrl: String,
        target: WallpaperTarget,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val result = downloadImage(context, imageUrl)
        result.onSuccess { file ->
            try {
                val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                
                when (target) {
                    WallpaperTarget.HOME -> {
                        wallpaperManager.setBitmap(bitmap, null, true, android.app.WallpaperManager.FLAG_SYSTEM)
                    }
                    WallpaperTarget.LOCK -> {
                        wallpaperManager.setBitmap(bitmap, null, true, android.app.WallpaperManager.FLAG_LOCK)
                    }
                    WallpaperTarget.BOTH -> {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                
                bitmap.recycle()
                file.delete()
                AnalyticsService.trackWallpaperSet(context, "gallery")
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Set wallpaper failed", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed")
                }
            }
        }.onFailure { error ->
            withContext(Dispatchers.Main) {
                onError(error.message ?: "Download failed")
            }
        }
    }

    // ==================== API Models ====================

    data class GalleryResponse(
        @SerializedName("images") val images: List<GalleryImage>,
        @SerializedName("pagination") val pagination: Pagination
    )

    data class GalleryImage(
        @SerializedName("id") val id: Int,
        @SerializedName("filename") val filename: String,
        @SerializedName("title") val title: String?,
        @SerializedName("category") val category: String,
        @SerializedName("thumbnail_url") val thumbnailUrl: String,
        @SerializedName("full_url") val fullUrl: String,
        @SerializedName("width") val width: Int,
        @SerializedName("height") val height: Int,
        @SerializedName("size") val size: Int,
        @SerializedName("downloads") val downloads: Int
    )

    data class Category(
        @SerializedName("category") val category: String,
        @SerializedName("count") val count: Int
    )

    data class Pagination(
        @SerializedName("page") val page: Int,
        @SerializedName("limit") val limit: Int,
        @SerializedName("total") val total: Int,
        @SerializedName("total_pages") val totalPages: Int
    )

    enum class WallpaperTarget {
        HOME, LOCK, BOTH
    }

    // ==================== API Interface ====================

    interface GalleryApi {
        @GET("api/gallery")
        fun getImages(
            @Query("category") category: String?,
            @Query("page") page: Int,
            @Query("limit") limit: Int
        ): retrofit2.Call<GalleryResponse>
        
        @GET("api/gallery/categories")
        fun getCategories(): retrofit2.Call<CategoriesResponse>
        
        @GET
        @retrofit2.http.Streaming
        fun downloadImage(@Url url: String): retrofit2.Call<okhttp3.ResponseBody>
    }

    data class CategoriesResponse(val categories: List<Category>)
}
