package com.example.pixlwallo.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pixlwallo.data.SelectionRepository
import com.example.pixlwallo.ui.components.ImagePreviewPager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember(context) { SelectionRepository(context) }
    val selected = remember { mutableStateListOf<Uri>() }
    val scope = rememberCoroutineScope()
    val isScanning = remember { mutableStateOf(false) }
    val previewIndex = remember { mutableStateOf<Int?>(null) }
    // 多选模式相关状态
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedForDeletion by remember { mutableStateOf(setOf<Uri>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    if (!selected.contains(uri)) {
                        selected.add(uri)
                        repo.takePersistable(uri)
                    }
                }
            }
        }

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri != null) {
                scope.launch {
                    isScanning.value = true
                    try {
                        // 获取文件夹的持久化权限
                        repo.takePersistableTree(treeUri)

                        // 递归扫描文件夹中的所有图片
                        val images = repo.scanImagesFromFolder(treeUri)

                        // 将找到的图片添加到选择列表
                        images.forEach { uri ->
                            if (!selected.contains(uri)) {
                                selected.add(uri)
                                repo.takePersistable(uri)
                            }
                        }
                    } finally {
                        isScanning.value = false
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        repo.selectedFlow.collect { list ->
            selected.clear()
            selected.addAll(list)
        }
    }

    // 在选择模式下拦截返回键，退出选择模式而不是关闭页面
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedForDeletion = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            "已选择 ${selectedForDeletion.size} 张",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    } else {
                        Text(
                            "选择图片",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedForDeletion = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // 选择模式下的操作按钮
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 全选/取消全选按钮
                            TextButton(
                                onClick = {
                                    if (selectedForDeletion.size == selected.size) {
                                        selectedForDeletion = emptySet()
                                    } else {
                                        selectedForDeletion = selected.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    if (selectedForDeletion.size == selected.size) "取消全选" else "全选"
                                )
                            }
                            // 删除按钮
                            IconButton(
                                onClick = {
                                    if (selectedForDeletion.isNotEmpty()) {
                                        showDeleteConfirm = true
                                    }
                                },
                                enabled = selectedForDeletion.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = if (selectedForDeletion.isNotEmpty()) 
                                        MaterialTheme.colorScheme.error 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    } else {
                        // 正常模式下的操作按钮
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                                Text("相册")
                            }
                            Button(
                                onClick = {
                                    folderPicker.launch(null)
                                },
                                enabled = !isScanning.value,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isScanning.value) "扫描中..." else "文件夹")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            repo.setSelection(selected.toList())
                            nav.popBackStack()
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "保存")
                }
            }
        }
    ) { inner ->
        if (isScanning.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        "正在扫描文件夹中的图片...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            if (selected.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "还没有选择图片",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "点击此处或上方按钮添加图片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)) {
                    // 根据屏幕宽度自适应计算每行显示多少个图片
                    // 假设每个图片最小宽度120dp，间距12dp，左右padding 12dp*2=24dp
                    val minItemWidth = 120.dp
                    val spacing = 12.dp
                    val horizontalPadding = 24.dp // 左右各12dp
                    val availableWidth = maxWidth - horizontalPadding
                    val itemsPerRow =
                        ((availableWidth + spacing) / (minItemWidth + spacing)).toInt()
                            .coerceAtLeast(2)

                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val rows = selected.chunked(itemsPerRow)

                        items(rows.size) { rowIndex ->
                            val rowItems = rows[rowIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                rowItems.forEach { uri ->
                                    val isSelected = selectedForDeletion.contains(uri)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        // 选择模式下：切换选择状态
                                                        selectedForDeletion = if (isSelected) {
                                                            selectedForDeletion - uri
                                                        } else {
                                                            selectedForDeletion + uri
                                                        }
                                                    } else {
                                                        // 正常模式：预览图片
                                                        val index = selected.indexOf(uri)
                                                        if (index >= 0) {
                                                            previewIndex.value = index
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isSelectionMode) {
                                                        // 长按进入选择模式
                                                        isSelectionMode = true
                                                        selectedForDeletion = selectedForDeletion + uri
                                                    }
                                                }
                                            )
//                                            .then(
//                                                if (isSelectionMode) {
//                                                    Modifier.border(
//                                                        width = if (isSelected) 4.dp else 2.dp,
//                                                        color = if (isSelected)
//                                                            MaterialTheme.colorScheme.primary
//                                                        else
//                                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
//                                                        shape = RoundedCornerShape(12.dp)
//                                                    )
//                                                } else {
//                                                    Modifier
//                                                }
//                                            )

                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // 选择模式下的选中覆盖层
                                        if (isSelectionMode && isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
//                                                        shape = RoundedCornerShape(12.dp)
                                                    ),
                                                contentAlignment = Alignment.TopEnd
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .size(24.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.primary,
                                                            shape = RoundedCornerShape(12.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // 如果这一行不满itemsPerRow个，添加空白占位
                                repeat(itemsPerRow - rowItems.size) {
                                    Box(modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 图片预览对话框 - 全屏预览，样式和预览页面一致
    previewIndex.value?.let { startIndex ->
        Dialog(
            onDismissRequest = { previewIndex.value = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false // 全屏显示
            )
        ) {
            // 设置 Dialog 窗口为全屏并隐藏系统栏
            val view = LocalView.current
            val activity = LocalContext.current as? Activity
            DisposableEffect(Unit) {
                if (activity != null) {
                    val window = activity.window
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    val controller = WindowInsetsControllerCompat(window, view)
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                }
                onDispose {
                    if (activity != null) {
                        val controller = WindowInsetsControllerCompat(activity.window, view)
                        controller.show(WindowInsetsCompat.Type.systemBars())
                        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                ImagePreviewPager(
                    images = selected,
                    initialPage = startIndex,
                    autoPlay = false, // 选择图片预览不自动播放
                    showExif = true,
                    onDismiss = { previewIndex.value = null }
                )
                
                IconButton(
                    onClick = { previewIndex.value = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // 批量删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除图片") },
            text = { 
                Text(
                    if (selectedForDeletion.size == 1) 
                        "确定要删除这张图片吗？" 
                    else 
                        "确定要删除这 ${selectedForDeletion.size} 张图片吗？"
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        selected.removeAll(selectedForDeletion)
                        selectedForDeletion = emptySet()
                        isSelectionMode = false
                        showDeleteConfirm = false
                        // 立即更新持久化存储
                        scope.launch {
                            repo.setSelection(selected.toList())
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}