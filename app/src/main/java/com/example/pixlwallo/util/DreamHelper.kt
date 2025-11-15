package com.example.pixlwallo.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.dreams.DreamService

object DreamHelper {
    /**
     * 打开屏保设置页面
     */
    fun openDreamSettings(context: Context) {
        val intent = Intent(Settings.ACTION_DREAM_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 检查当前应用是否是默认屏保
     * 注意：Android API没有直接的方法检查，这里返回null表示无法确定
     */
    fun isCurrentDream(context: Context): Boolean? {
        // Android API限制，无法直接查询当前启用的Dream
        // 用户需要在系统设置中手动启用
        return null
    }

    /**
     * 获取应用的Dream组件名称
     */
    fun getDreamComponentName(context: Context): ComponentName {
        return ComponentName(context, "com.example.pixlwallo.dream.PhotoDreamService")
    }
}

