package com.example.pixlwallo.slideshow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pixlwallo.R

object Notifications {
    const val CHANNEL_ID = "slideshow_channel"
    const val CHANNEL_NAME = "Wallpaper Slideshow"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
                ch.enableLights(false)
                ch.enableVibration(false)
                ch.lightColor = Color.BLUE
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun ongoing(context: Context, title: String, text: String, actions: List<NotificationCompat.Action> = emptyList()): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        actions.forEach { builder.addAction(it) }
        return builder.build()
    }
}