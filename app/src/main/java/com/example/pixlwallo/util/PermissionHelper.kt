package com.example.pixlwallo.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object PermissionHelper {
    /**
     * 检查是否有悬浮窗权限（SYSTEM_ALERT_WINDOW）
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Android 6.0以下默认有权限
        }
    }

    /**
     * 打开悬浮窗权限设置页面
     */
    fun openOverlayPermissionSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 检查是否有锁屏显示权限（USE_FULL_SCREEN_INTENT）
     * 注意：这个权限在Android 10+需要用户手动授予，但无法通过代码直接检查
     * 通常通过尝试使用全屏Intent来验证
     */
    fun hasFullScreenIntentPermission(context: Context): Boolean {
        // Android 10+需要检查，但无法直接查询
        // 通常这个权限在应用安装时默认授予，但用户可以在设置中撤销
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 对于Android 10+，需要用户手动在设置中授予
            // 这里返回true，实际使用时需要捕获SecurityException
            true
        } else {
            true
        }
    }

    /**
     * 打开应用通知设置页面（可以管理全屏Intent权限）
     */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

