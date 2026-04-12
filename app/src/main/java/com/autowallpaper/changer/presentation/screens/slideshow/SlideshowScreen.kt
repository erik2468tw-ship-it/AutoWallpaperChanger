package com.autowallpaper.changer.presentation.screens.slideshow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.autowallpaper.changer.domain.model.WallpaperItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlideshowScreen(
    images: List<WallpaperItem>,
    intervalSeconds: Int = 5,
    onExit: () -> Unit
) {
    if (images.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("沒有圖片可播放", color = Color.White)
        }
        return
    }

    // 預先載入所有圖片以加快速度
    val shuffledImages = remember { images.shuffled() }
    
    val pagerState = rememberPagerState(
        pageCount = { shuffledImages.size },
        initialPage = 0
    )
    val context = LocalContext.current
    
    // 播放/暫停狀態
    var isPlaying by remember { mutableStateOf(true) }

    // Auto-scroll with random order
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay((intervalSeconds * 1000).toLong())
            if (shuffledImages.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % shuffledImages.size
                pagerState.scrollToPage(nextPage)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { isPlaying = !isPlaying }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp
        ) { page ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(shuffledImages[page].uri)
                    .crossfade(true)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
