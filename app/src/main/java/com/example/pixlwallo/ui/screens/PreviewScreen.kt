package com.example.pixlwallo.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.pixlwallo.data.SelectionRepository
import com.example.pixlwallo.data.SettingsRepository
import com.example.pixlwallo.model.PlaybackConfig
import com.example.pixlwallo.model.PlaybackOrder
import com.example.pixlwallo.model.OrientationFilter
import com.example.pixlwallo.ui.components.ImagePreviewPager
import com.example.pixlwallo.ui.immersive.ImmersiveMode
import com.example.pixlwallo.util.ExifReader
import kotlin.random.Random
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.net.Uri

@Composable
fun PreviewScreen(nav: NavController) {
    val context = LocalContext.current
    
    // 强制横屏
    val activity = context as? Activity
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val repo = remember(context) { SelectionRepository(context) }
    val settingsRepo = remember(context) { SettingsRepository(context) }
    val images by repo.selectedFlow.collectAsState(initial = emptyList())
    val cfg by settingsRepo.configFlow.collectAsState(initial = PlaybackConfig())
    val orientationFilter by settingsRepo.orientationFilterFlow.collectAsState(initial = OrientationFilter.ALL)
    val scope = rememberCoroutineScope()
    
    // 图片方向缓存（Uri -> true表示横屏，false表示竖屏，null表示未知）
    val imageOrientations = remember { mutableMapOf<Uri, Boolean?>() }
    // 用于触发重新计算的计数器
    var orientationUpdateTrigger by remember { mutableStateOf(0) }
    
    // 异步加载图片方向信息
    LaunchedEffect(images.size, images.joinToString { it.toString() }) {
        images.forEach { uri ->
            if (!imageOrientations.containsKey(uri)) {
                // 先设置为null表示正在加载
                imageOrientations[uri] = null
                // 触发重新计算，让null的图片先显示
                orientationUpdateTrigger++
                scope.launch {
                    val exifInfo = ExifReader.readExif(context, uri)
                    val isLandscape = exifInfo?.let { info ->
                        val width = info.width ?: 0
                        val height = info.height ?: 0
                        if (width > 0 && height > 0) {
                            width > height
                        } else {
                            null
                        }
                    }
                    imageOrientations[uri] = isLandscape
                    // 触发重新计算
                    orientationUpdateTrigger++
                }
            }
        }
    }
    
    // 根据过滤器过滤图片
    val filteredImages = remember(images, orientationFilter, orientationUpdateTrigger) {
        when (orientationFilter) {
            OrientationFilter.ALL -> images
            OrientationFilter.LANDSCAPE -> images.filter { uri ->
                // 如果方向信息还未加载（null），暂时显示该图片
                val orientation = imageOrientations[uri]
                orientation == true || orientation == null
            }
            OrientationFilter.PORTRAIT -> images.filter { uri ->
                // 如果方向信息还未加载（null），暂时显示该图片
                val orientation = imageOrientations[uri]
                orientation == false || orientation == null
            }
        }
    }
    
    // 使用一个key来跟踪页面进入，确保每次进入时都重新生成随机顺序
    var screenKey by remember { mutableStateOf(0) }
    
    // 检测组件创建，每次创建时更新key（包括首次创建和重新创建）
    DisposableEffect(Unit) {
        screenKey++
        onDispose { }
    }
    
    // 根据配置直接打乱 filteredImages（如果是随机模式）
    val displayImages = remember(filteredImages, cfg.order, screenKey) {
        if (cfg.order == PlaybackOrder.RANDOM && filteredImages.isNotEmpty()) {
            filteredImages.shuffled(Random(System.currentTimeMillis()))
        } else {
            filteredImages
        }
    }

    ImmersiveMode(enabled = true)

    // 使用key来强制重新创建ImagePreviewPager，确保随机顺序重新生成
    key(screenKey) {
        ImagePreviewPager(
            images = displayImages,
            initialPage = 0,
            autoPlay = true, // 预览页面自动播放
            showExif = true
        )
    }
}

