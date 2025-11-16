package com.example.pixlwallo.ui.screens

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
import com.example.pixlwallo.ui.components.ImagePreviewPager
import com.example.pixlwallo.ui.immersive.ImmersiveMode
import kotlin.random.Random

@Composable
fun PreviewScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember(context) { SelectionRepository(context) }
    val settingsRepo = remember(context) { SettingsRepository(context) }
    val images by repo.selectedFlow.collectAsState(initial = emptyList())
    val cfg by settingsRepo.configFlow.collectAsState(initial = PlaybackConfig())
    
    // 使用一个key来跟踪页面进入，确保每次进入时都重新生成随机顺序
    var screenKey by remember { mutableStateOf(0) }
    
    // 检测组件创建，每次创建时更新key（包括首次创建和重新创建）
    DisposableEffect(Unit) {
        screenKey++
        onDispose { }
    }
    
    // 根据配置直接打乱 images（如果是随机模式）
    val displayImages = remember(images, cfg.order, screenKey) {
        if (cfg.order == PlaybackOrder.RANDOM && images.isNotEmpty()) {
            images.shuffled(Random(System.currentTimeMillis()))
        } else {
            images
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

