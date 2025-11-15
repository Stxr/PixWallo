package com.example.pixlwallo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pixlwallo.data.SettingsRepository
import com.example.pixlwallo.model.ApplyScope
import com.example.pixlwallo.model.ExifPosition
import com.example.pixlwallo.model.PlaybackConfig
import com.example.pixlwallo.model.PlaybackOrder
import com.example.pixlwallo.util.PermissionHelper
import com.example.pixlwallo.util.DreamHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// 预定义的时长值（毫秒）
private val durationValues = listOf(
    1000L,      // 1秒
    5000L,      // 5秒
    15000L,     // 15秒
    30000L,     // 30秒
    60000L,     // 60秒
    300000L,    // 5分钟
    600000L,    // 10分钟
    900000L,    // 15分钟
    1800000L,   // 30分钟
    3600000L,   // 60分钟
    7200000L    // 120分钟
)

// 将毫秒值转换为滑动条索引（找到最接近的值）
private fun msToSliderIndex(ms: Long): Int {
    var closestIndex = 0
    var minDiff = kotlin.math.abs(durationValues[0] - ms)
    
    durationValues.forEachIndexed { index, value ->
        val diff = kotlin.math.abs(value - ms)
        if (diff < minDiff) {
            minDiff = diff
            closestIndex = index
        }
    }
    
    return closestIndex
}

// 格式化显示文本
private fun formatDuration(ms: Long): String {
    return when {
        ms < 60000 -> "${ms / 1000}秒"
        else -> "${ms / 60000}分钟"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember(context) { SettingsRepository(context) }
    var cfg by remember { mutableStateOf(PlaybackConfig()) }
    var sliderValue by remember { mutableStateOf(0f) }
    var overlayPermissionGranted by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }
    var fullScreenPermissionGranted by remember { mutableStateOf(PermissionHelper.hasFullScreenIntentPermission(context)) }

    LaunchedEffect(Unit) {
        repo.configFlow.collectLatest { 
            cfg = it
            sliderValue = msToSliderIndex(it.perItemMs).toFloat()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 每张时长设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "每张时长",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(durationValues[sliderValue.toInt().coerceIn(0, durationValues.size - 1)]),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Slider(
                            value = sliderValue,
                            onValueChange = { newValue ->
                                val index = newValue.toInt().coerceIn(0, durationValues.size - 1)
                                sliderValue = index.toFloat()
                                scope.launch {
                                    repo.update { it.copy(perItemMs = durationValues[index]) }
                                }
                            },
                            valueRange = 0f..(durationValues.size - 1).toFloat(),
                            steps = durationValues.size - 2
                        )
                    }
                }
            }
            
            // 播放顺序设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "播放顺序",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilledTonalButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (cfg.order == PlaybackOrder.RANDOM) "随机播放" else "按选中顺序",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("按选中顺序") },
                                onClick = {
                                    expanded = false
                                    runBlocking { repo.update { it.copy(order = PlaybackOrder.SELECTED) } }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("随机播放") },
                                onClick = {
                                    expanded = false
                                    runBlocking { repo.update { it.copy(order = PlaybackOrder.RANDOM) } }
                                }
                            )
                        }
                    }
                }
            }
            
            // 应用范围设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "应用范围",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    var expandedScope by remember { mutableStateOf(false) }
                    Box {
                        FilledTonalButton(
                            onClick = { expandedScope = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                when (cfg.applyScope) {
                                    ApplyScope.LOCK -> "锁屏"
                                    ApplyScope.SYSTEM -> "主屏"
                                    ApplyScope.BOTH -> "锁屏和主屏"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedScope,
                            onDismissRequest = { expandedScope = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("锁屏") },
                                onClick = {
                                    expandedScope = false
                                    runBlocking { repo.update { it.copy(applyScope = ApplyScope.LOCK) } }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("主屏") },
                                onClick = {
                                    expandedScope = false
                                    runBlocking { repo.update { it.copy(applyScope = ApplyScope.SYSTEM) } }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("锁屏和主屏") },
                                onClick = {
                                    expandedScope = false
                                    runBlocking { repo.update { it.copy(applyScope = ApplyScope.BOTH) } }
                                }
                            )
                        }
                    }
                }
            }
            
            // 权限管理
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "应用权限",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // 悬浮窗权限
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "悬浮窗权限",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (overlayPermissionGranted) "已授予" else "未授予",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overlayPermissionGranted) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                PermissionHelper.openOverlayPermissionSettings(context)
                                // 延迟检查权限状态更新
                                scope.launch {
                                    delay(500)
                                    overlayPermissionGranted = PermissionHelper.canDrawOverlays(context)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (!overlayPermissionGranted) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(6.dp))
                            }
                            Text(if (overlayPermissionGranted) "管理" else "授予")
                        }
                    }
                    
                    // 锁屏显示权限
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "锁屏显示权限",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "允许在锁屏上显示内容",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                PermissionHelper.openNotificationSettings(context)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("管理")
                        }
                    }
                }
            }
            
            // 锁屏屏保设置（StandBy模式）
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "锁屏屏保（StandBy模式）",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        "在锁屏时自动显示图片轮播和时间日期，类似StandBy模式。需要在系统设置中启用屏保功能。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    FilledTonalButton(
                        onClick = {
                            DreamHelper.openDreamSettings(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("打开屏保设置")
                    }
                    
                    Text(
                        "提示：在屏保设置中选择「PixlWallo\"，然后启用「在充电时启动」或「在基座上启动」。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // EXIF 信息设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "EXIF 信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // 启用轻触显示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "轻触显示 EXIF",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "轻触图片显示 EXIF 信息",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = cfg.enableExifTap,
                            onCheckedChange = { enabled ->
                                runBlocking { 
                                    repo.update { it.copy(enableExifTap = enabled) }
                                }
                            }
                        )
                    }
                    
                    // EXIF 信息位置
                    var expandedExifPos by remember { mutableStateOf(false) }
                    Box {
                        FilledTonalButton(
                            onClick = { expandedExifPos = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = cfg.enableExifTap
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                when (cfg.exifPosition) {
                                    ExifPosition.TOP_LEFT -> "左上角"
                                    ExifPosition.TOP_RIGHT -> "右上角"
                                    ExifPosition.BOTTOM_LEFT -> "左下角"
                                    ExifPosition.BOTTOM_RIGHT -> "右下角"
                                    ExifPosition.CENTER -> "中间"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedExifPos,
                            onDismissRequest = { expandedExifPos = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("左上角") },
                                onClick = {
                                    expandedExifPos = false
                                    runBlocking { repo.update { it.copy(exifPosition = ExifPosition.TOP_LEFT) } }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("右上角") },
                                onClick = {
                                    expandedExifPos = false
                                    runBlocking { repo.update { it.copy(exifPosition = ExifPosition.TOP_RIGHT) } }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("左下角") },
                                onClick = {
                                    expandedExifPos = false
                                    runBlocking { repo.update { it.copy(exifPosition = ExifPosition.BOTTOM_LEFT) } }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("右下角") },
                                onClick = {
                                    expandedExifPos = false
                                    runBlocking { repo.update { it.copy(exifPosition = ExifPosition.BOTTOM_RIGHT) } }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("中间") },
                                onClick = {
                                    expandedExifPos = false
                                    runBlocking { repo.update { it.copy(exifPosition = ExifPosition.CENTER) } }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}