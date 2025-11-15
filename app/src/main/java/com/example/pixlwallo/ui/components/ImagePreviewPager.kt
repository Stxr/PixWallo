package com.example.pixlwallo.ui.components

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pixlwallo.data.SettingsRepository
import com.example.pixlwallo.model.ExifPosition
import com.example.pixlwallo.model.PlaybackConfig
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pixlwallo.util.ExifInfo
import com.example.pixlwallo.util.ExifReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding

/**
 * 可复用的图片预览组件
 * @param images 图片 URI 列表
 * @param initialPage 初始页面索引
 * @param autoPlay 是否自动播放（自动切换）
 * @param showExif 是否显示 EXIF 信息（点击切换）
 * @param onDismiss 关闭回调（用于 Dialog 场景）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewPager(
    images: List<Uri>,
    initialPage: Int = 0,
    autoPlay: Boolean = false,
    showExif: Boolean = true,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsRepo = remember(context) { SettingsRepository(context) }
    val cfg by settingsRepo.configFlow.collectAsState(initial = PlaybackConfig())
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, images.size.coerceAtLeast(1) - 1),
        pageCount = { images.size.coerceAtLeast(1) }
    )
    val errorUris = remember { mutableStateMapOf<Int, Boolean>() }
    var showExifInfo by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf<ExifInfo?>(null) }
    val exifCache = remember { mutableStateMapOf<Int, ExifInfo?>() }
    val scope = rememberCoroutineScope()

    // 自动播放逻辑
    LaunchedEffect(images, cfg.perItemMs, autoPlay) {
        if (!autoPlay || images.isEmpty()) return@LaunchedEffect
        while (isActive && autoPlay) {
            delay(cfg.perItemMs)
            val next = (pagerState.currentPage + 1) % images.size
            if (images.isNotEmpty()) pagerState.animateScrollToPage(next)
        }
    }

    // 预加载当前页和下一页的 EXIF 信息
    LaunchedEffect(pagerState.currentPage, images) {
        if (images.isEmpty()) return@LaunchedEffect
        val currentPage = pagerState.currentPage
        val nextPage = (currentPage + 1) % images.size
        
        // 切换页面时隐藏 EXIF 信息
        showExifInfo = false
        
        listOf(currentPage, nextPage).forEach { page ->
            if (page < images.size && !exifCache.containsKey(page)) {
                scope.launch {
                    val exif = ExifReader.readExif(context, images[page])
                    exifCache[page] = exif
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(showExif && cfg.enableExifTap, pagerState.currentPage) {
                if (showExif && cfg.enableExifTap) {
                    detectTapGestures(
                        onTap = {
                            val currentPage = pagerState.currentPage
                            val exif = exifCache[currentPage]
                            if (exif != null) {
                                currentExif = exif
                                showExifInfo = !showExifInfo
                            } else {
                                // 如果缓存中没有，尝试加载
                                scope.launch {
                                    val loadedExif = ExifReader.readExif(context, images[currentPage])
                                    exifCache[currentPage] = loadedExif
                                    if (loadedExif != null) {
                                        currentExif = loadedExif
                                        showExifInfo = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
    ) {
        if (images.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("请先选择图片", color = Color.White)
            }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val hasError = errorUris[page] == true
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasError) {
                        androidx.compose.material3.Text("无法加载图片", color = Color.White)
                    } else {
                        AsyncImage(
                            model = images[page],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = {
                                errorUris[page] = true
                            },
                            onSuccess = {
                                errorUris.remove(page)
                            }
                        )
                    }
                }
            }
            
            // EXIF 信息悬浮显示（显示在当前页面上）
            if (showExifInfo && currentExif != null && showExif && cfg.enableExifTap) {
                AnimatedVisibility(
                    visible = showExifInfo,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(getAlignmentForPosition(cfg.exifPosition))
                ) {
                    ExifInfoCard(
                        exif = currentExif!!,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExifInfoCard(exif: ExifInfo, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.5f)
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            exif.camera?.let {
                ExifRow("相机", it)
            }
            exif.dateTime?.let {
                ExifRow("时间", it)
            }
            exif.location?.let {
                ExifRow("位置", it)
            }
            exif.iso?.let {
                ExifRow("ISO", it)
            }
            exif.aperture?.let {
                ExifRow("光圈", it)
            }
            exif.exposureTime?.let {
                ExifRow("快门", it)
            }
            exif.focalLength?.let {
                ExifRow("焦距", it)
            }
            exif.flash?.let {
                ExifRow("闪光灯", it)
            }
            if (exif.width != null && exif.height != null) {
                ExifRow("尺寸", "${exif.width} × ${exif.height}")
            }
        }
    }
}

@Composable
private fun ExifRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

internal fun getAlignmentForPosition(position: ExifPosition): Alignment {
    return when (position) {
        ExifPosition.TOP_LEFT -> Alignment.TopStart
        ExifPosition.TOP_RIGHT -> Alignment.TopEnd
        ExifPosition.BOTTOM_LEFT -> Alignment.BottomStart
        ExifPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        ExifPosition.CENTER -> Alignment.Center
    }
}

