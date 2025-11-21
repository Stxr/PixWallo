package com.example.pixlwallo.model

import android.net.Uri

data class SelectedImage(
    val uri: Uri,
    val addedAt: Long
)

enum class PlaybackOrder { SELECTED, RANDOM }

enum class ApplyScope { LOCK, SYSTEM, BOTH }

enum class ExifPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER }

enum class ImgDisplayMode {
    FILL_SCREEN, // 填满屏幕 (竖屏照片会被裁剪或放大)
    FIT_CENTER,  // 完整显示 (竖屏照片两侧留黑)
    SMART        // 智能模式 (竖屏照片自动旋转90度填满屏幕)
}

enum class OrientationFilter {
    ALL,      // 全部
    LANDSCAPE, // 横屏
    PORTRAIT   // 竖屏
}

data class PlaybackConfig(
    val perItemMs: Long = 10_000,
    val maxDurationMs: Long? = null,
    val idleStopMs: Long? = 30 * 60_000,
    val order: PlaybackOrder = PlaybackOrder.SELECTED,
    val applyScope: ApplyScope = ApplyScope.LOCK,
    val enableExifTap: Boolean = true,
    val exifPosition: ExifPosition = ExifPosition.BOTTOM_RIGHT,
    val imgDisplayMode: ImgDisplayMode = ImgDisplayMode.FILL_SCREEN
)

data class PlaybackState(
    val index: Int = 0,
    val isPlaying: Boolean = false,
    val startedAt: Long = 0L,
    val lastUserActionAt: Long = 0L
)