package com.autowallpaper.changer.domain.model

import android.net.Uri

data class WallpaperItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val dateTaken: Long,
    val folderPath: String,
    val tags: List<String> = emptyList()
)
