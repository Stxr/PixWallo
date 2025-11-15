package com.example.pixlwallo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.pixlwallo.data.SelectionRepository
import com.example.pixlwallo.ui.components.ImagePreviewPager
import com.example.pixlwallo.ui.immersive.ImmersiveMode

@Composable
fun PreviewScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember(context) { SelectionRepository(context) }
    val images by repo.selectedFlow.collectAsState(initial = emptyList())

    ImmersiveMode(enabled = true)

    ImagePreviewPager(
        images = images,
        initialPage = 0,
        autoPlay = true, // 预览页面自动播放
        showExif = true
    )
}

