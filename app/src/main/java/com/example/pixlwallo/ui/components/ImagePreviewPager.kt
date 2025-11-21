package com.example.pixlwallo.ui.components

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.rotate
import android.graphics.Bitmap
import android.graphics.Matrix
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.isSpecified
import com.example.pixlwallo.model.ImgDisplayMode

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

    val pagerState =
        rememberPagerState(initialPage = initialPage.coerceIn(0, images.size.coerceAtLeast(1) - 1),
            pageCount = { images.size.coerceAtLeast(1) * 1000 })

    val errorUris = remember { mutableStateMapOf<Int, Boolean>() }
    var showExifInfo by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf<ExifInfo?>(null) }
    val exifCache = remember { mutableStateMapOf<Int, ExifInfo?>() }
    val scope = rememberCoroutineScope()

    // 跟踪是否是用户手动滑动
    var isUserSwipe by remember { mutableStateOf(false) }
    var autoPlayJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 监听页面变化，检测用户手动滑动
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.distinctUntilChanged().filter { it }
            .collect {
                // 用户开始滑动，标记为用户滑动
                // 不立即取消 Job，让循环自然检查 isScrollInProgress 来跳过切换
                isUserSwipe = true
            }
    }

    // 监听滑动结束，恢复自动播放
    LaunchedEffect(pagerState, autoPlay, cfg.perItemMs, images) {
        snapshotFlow { pagerState.isScrollInProgress }.distinctUntilChanged()
            .filter { !it && isUserSwipe }.collect {
                // 用户滑动结束，重置标志
                isUserSwipe = false
                // 重新启动自动播放（如果启用）
                if (autoPlay && images.isNotEmpty()) {
                    autoPlayJob?.cancel()
                    autoPlayJob = scope.launch {
                        // 初始延迟
                        delay(cfg.perItemMs)

                        while (isActive && autoPlay && images.isNotEmpty()) {
                            // 检查是否正在滑动
                            if (!pagerState.isScrollInProgress) {
                                moveToNextPage(pagerState, images)
                            }
                            // 等待指定时间后继续下一次
                            delay(cfg.perItemMs)
                        }
                    }
                }
            }
    }

    // 自动播放逻辑（初始启动）
    LaunchedEffect(images, cfg.perItemMs, autoPlay) {
        if (!autoPlay || images.isEmpty()) {
            autoPlayJob?.cancel()
            autoPlayJob = null
            return@LaunchedEffect
        }

        // 取消之前的任务
        autoPlayJob?.cancel()

        // 如果用户正在滑动，等待滑动结束
        if (pagerState.isScrollInProgress) return@LaunchedEffect

        autoPlayJob = scope.launch {
            // 初始延迟
            delay(cfg.perItemMs)

            while (isActive && autoPlay && images.isNotEmpty()) {
                // 检查是否正在滑动
                if (!pagerState.isScrollInProgress) {
                    moveToNextPage(pagerState, images)
                }
                // 等待指定时间后继续下一次
                delay(cfg.perItemMs)
            }
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


    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(showExif && cfg.enableExifTap, pagerState.currentPage) {
            if (showExif && cfg.enableExifTap) {
                detectTapGestures(onTap = {
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
                })
            }
        }) {
        if (images.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("请先选择图片", color = Color.White)
            }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val hasError = errorUris[page] == true

                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    if (hasError) {
                        Text("无法加载图片", color = Color.White)
                    } else {
                        val currentImage = images[page % images.size]
                        
                        // 使用状态来跟踪是否需要旋转
                        var shouldRotate by remember(page, cfg.imgDisplayMode, currentImage) { 
                            mutableStateOf<Boolean?>(null) 
                        }
                        
                        // 当图片或配置改变时，尝试从 EXIF 缓存中预先判断尺寸
                        LaunchedEffect(currentImage, cfg.imgDisplayMode, exifCache[page]) {
                            shouldRotate = null
                            // 如果 EXIF 缓存中有尺寸信息，立即判断是否需要旋转
                            val exif = exifCache[page]
                            if (exif != null && exif.width != null && exif.height != null) {
                                if (exif.height > exif.width && cfg.imgDisplayMode == ImgDisplayMode.SMART) {
                                    shouldRotate = true
                                } else {
                                    shouldRotate = false
                                }
                            }
                        }
                        
                        // 根据 shouldRotate 状态决定使用哪个 model
                        // 使用 remember 缓存，只在真正需要时重新计算
                        val imageModel = remember(currentImage.toString(), shouldRotate) {
                            when (shouldRotate) {
                                true -> {
                                    // 需要在加载时旋转
                                    ImageRequest.Builder(context)
                                        .data(currentImage)
                                        .transformations(RotationTransformation(-90f))
                                        .build()
                                }
                                false, null -> {
                                    // 不需要旋转或初始状态，使用普通加载
                                    currentImage
                                }
                            }
                        }
                        
                        // rememberAsyncImagePainter 会根据 model 的变化自动处理
                        val painter = rememberAsyncImagePainter(
                            model = imageModel,
                            onState = { state ->
                                when (state) {
                                    is AsyncImagePainter.State.Error -> errorUris[page] = true
                                    is AsyncImagePainter.State.Success -> {
                                        errorUris.remove(page)
                                        // 如果 EXIF 信息还没有，从加载的图片中检查尺寸
                                        if (shouldRotate == null) {
                                            val size = state.painter.intrinsicSize
                                            if (size.isSpecified && size.height > size.width && cfg.imgDisplayMode == ImgDisplayMode.SMART) {
                                                shouldRotate = true
                                            } else {
                                                shouldRotate = false
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        )

                        var modifier = Modifier.fillMaxSize()
                        val contentScale: ContentScale

                        if (painter.state is AsyncImagePainter.State.Success) {
                            val size = painter.intrinsicSize
                            // 如果已经旋转，尺寸会交换，所以需要检查旋转后的尺寸
                            val isPortrait = if (shouldRotate == true) {
                                // 已旋转，原来的竖屏现在变成横屏
                                size.isSpecified && size.width > size.height
                            } else {
                                size.isSpecified && size.height > size.width
                            }
                            
                            if (isPortrait && shouldRotate != true) {
                                // 竖屏照片（未旋转）：根据配置决定显示方式
                                contentScale = when (cfg.imgDisplayMode) {
                                    ImgDisplayMode.FILL_SCREEN -> ContentScale.Crop // 填满屏幕（裁剪）
                                    ImgDisplayMode.FIT_CENTER -> ContentScale.Fit   // 完整显示（留黑）
                                    ImgDisplayMode.SMART -> {
                                        // 智能模式：已在加载时旋转，直接填满屏幕
                                        ContentScale.Crop
                                    }
                                }
                            } else {
                                // 横屏照片或已旋转的竖屏照片：始终填满屏幕
                                contentScale = ContentScale.Crop
                            }
                        } else {
                            contentScale = ContentScale.Crop
                        }

                        Image(
                            painter = painter,
                            contentDescription = null,
                            contentScale = contentScale,
                            modifier = modifier
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
                        exif = currentExif!!, modifier = Modifier.padding(16.dp)
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
            text = value, color = Color.White, fontSize = 12.sp
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

/**
 * 移动到下一页
 */
private suspend fun moveToNextPage(
    pagerState: PagerState, images: List<Uri>
) {
    if (images.isEmpty()) return
    val currentPage = pagerState.currentPage
    val nextPage = (currentPage + 1) % images.size
    // 如果已经是最后一页且要循环到第一页，或者下一页和当前页不同
    if (nextPage != currentPage) {
        withContext(NonCancellable) {
            pagerState.animateScrollToPage(nextPage)
        }
    }
}

/**
 * 自定义旋转变换类
 */
private class RotationTransformation(private val degrees: Float) : Transformation {
    override val cacheKey: String
        get() = "RotationTransformation(degrees=$degrees)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
    }
}

