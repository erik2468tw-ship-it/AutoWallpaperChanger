package com.autowallpaper.changer.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.autowallpaper.changer.domain.model.WallpaperItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Scan a specific SAF tree folder for images
     */
    suspend fun scanSafFolder(folderUri: Uri): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val images = mutableListOf<WallpaperItem>()

        try {
            val documentFile = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()

            documentFile.listFiles().forEach { file ->
                if (file.isFile && isImageFile(file.name ?: "")) {
                    val uri = file.uri
                    val name = file.name ?: "Unknown"
                    val size = file.length()
                    val dateTaken = file.lastModified()

                    // Get image dimensions
                    val (width, height) = getImageDimensions(uri)

                    images.add(
                        WallpaperItem(
                            id = uri.hashCode().toLong(),
                            uri = uri,
                            displayName = name,
                            width = width,
                            height = height,
                            size = size,
                            dateTaken = dateTaken,
                            folderPath = folderUri.toString(),
                            tags = extractTags(name)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        images.sortedByDescending { it.dateTaken }
    }

    /**
     * Scan all images from MediaStore (fallback)
     */
    suspend fun scanAllImages(): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val images = mutableListOf<WallpaperItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATA
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateTaken = cursor.getLong(dateColumn)
                    val path = cursor.getString(dataColumn) ?: ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val tags = extractTags(name)

                    images.add(
                        WallpaperItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            width = width,
                            height = height,
                            size = size,
                            dateTaken = dateTaken,
                            folderPath = path,
                            tags = tags
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        images
    }

    private fun isImageFile(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".bmp") || lower.endsWith(".webp") ||
               lower.endsWith(".heic") || lower.endsWith(".heif")
    }

    private fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                Pair(options.outWidth, options.outHeight)
            } ?: Pair(0, 0)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    private fun extractTags(filename: String): List<String> {
        val tags = mutableListOf<String>()
        val lower = filename.lowercase()

        // Seasonal tags
        if (lower.contains("christmas") || lower.contains("xmas") || lower.contains("聖誕"))
            tags.add("christmas")
        if (lower.contains("halloween") || lower.contains("萬聖"))
            tags.add("halloween")
        if (lower.contains("spring") || lower.contains("春"))
            tags.add("spring")
        if (lower.contains("summer") || lower.contains("夏"))
            tags.add("summer")
        if (lower.contains("autumn") || lower.contains("秋"))
            tags.add("autumn")
        if (lower.contains("winter") || lower.contains("冬"))
            tags.add("winter")
        if (lower.contains("newyear") || lower.contains("新年") || lower.contains("春節"))
            tags.add("newyear")
        if (lower.contains("valentine") || lower.contains("情人"))
            tags.add("valentine")

        return tags
    }
}
