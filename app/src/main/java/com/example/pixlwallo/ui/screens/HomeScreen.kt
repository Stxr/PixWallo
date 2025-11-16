package com.example.pixlwallo.ui.screens

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.example.pixlwallo.data.SelectionRepository
import com.example.pixlwallo.data.SettingsRepository
import com.example.pixlwallo.model.PlaybackConfig
import com.example.pixlwallo.slideshow.SlideshowService
import kotlinx.coroutines.flow.map
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import android.provider.Settings
import android.content.Intent

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val selectionRepo = rememberSelectionRepo(context)
    val settingsRepo = rememberSettingsRepo(context)
    val selectedImages by selectionRepo.selectedFlow.collectAsState(initial = emptyList())
    val count = selectedImages.size
    val cfg by settingsRepo.configFlow.collectAsState(initial = PlaybackConfig())
    val notifPerm = if (Build.VERSION.SDK_INT >= 33) rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null
    
    // 随机选择一张图片作为背景
    var backgroundImageUri by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(selectedImages) {
        backgroundImageUri = if (selectedImages.isNotEmpty()) {
            selectedImages.random()
        } else {
            null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题
            Text(
                text = "PixlWallo",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 统计卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (backgroundImageUri != null) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // 背景图片（可点击，随机切换）
                    if (backgroundImageUri != null) {
                        AsyncImage(
                            model = backgroundImageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    // 随机切换背景图片
                                    if (selectedImages.isNotEmpty()) {
                                        backgroundImageUri = selectedImages.random()
                                    }
                                }
                        )
                        // 半透明遮罩层，确保文字可读性
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    }
                    
                    // 内容层
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // "已选图片"区域（可点击，跳转到选择图片页）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.clickable { nav.navigate("picker") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (backgroundImageUri != null) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "已选图片",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (backgroundImageUri != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$count 张",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (backgroundImageUri != null) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // 设置区域（可点击，跳转到设置页）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate("settings") },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "每张时长",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (backgroundImageUri != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDuration(cfg.perItemMs),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (backgroundImageUri != null) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "播放顺序",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (backgroundImageUri != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (cfg.order.name == "RANDOM") "随机" else "顺序",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (backgroundImageUri != null) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            
            // 主要操作按钮
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { nav.navigate("picker") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("选择图片")
                }
                
                OutlinedButton(
                    onClick = { nav.navigate("preview") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("电子相框")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 播放控制
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "播放控制",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (count == 0) {
                                    Toast.makeText(context, "请先选择至少一张图片", Toast.LENGTH_SHORT).show()
                                    return@FilledTonalButton
                                }
                                if (Build.VERSION.SDK_INT >= 33 && notifPerm?.status?.isGranted == false) {
                                    notifPerm.launchPermissionRequest()
                                    return@FilledTonalButton
                                }
                                SlideshowService.start(context)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("开始播放")
                        }
                        
                        OutlinedButton(
                            onClick = { SlideshowService.stop(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("停止播放")
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("打开屏保设置")
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSelectionRepo(context: Context) = androidx.compose.runtime.remember(context) { SelectionRepository(context) }

@Composable
private fun rememberSettingsRepo(context: Context) = androidx.compose.runtime.remember(context) { SettingsRepository(context) }

/**
 * 格式化时长显示，自动转换为秒、分钟、小时
 */
private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> {
            if (minutes > 0) {
                "${hours}小时${minutes}分钟"
            } else {
                "${hours}小时"
            }
        }
        minutes > 0 -> {
            if (seconds > 0) {
                "${minutes}分钟${seconds}秒"
            } else {
                "${minutes}分钟"
            }
        }
        else -> "${seconds}秒"
    }
}