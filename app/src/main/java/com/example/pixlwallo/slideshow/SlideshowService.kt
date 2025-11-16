package com.example.pixlwallo.slideshow

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.pixlwallo.data.SelectionRepository
import com.example.pixlwallo.data.SettingsRepository
import com.example.pixlwallo.model.ApplyScope
import com.example.pixlwallo.model.PlaybackConfig
import com.example.pixlwallo.model.PlaybackOrder
import com.example.pixlwallo.wallpaper.WallpaperApplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class SlideshowService : Service() {

    private lateinit var selectionRepo: SelectionRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var applier: WallpaperApplier

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        selectionRepo = SelectionRepository(applicationContext)
        settingsRepo = SettingsRepository(applicationContext)
        applier = WallpaperApplier(applicationContext)
        Notifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLoop()
            ACTION_STOP -> stopSelf()
            ACTION_NEXT -> applyNextOne()
            ACTION_PREV -> applyPrevOne()
            ACTION_TOGGLE -> toggle()
            else -> startLoop()
        }
        return START_STICKY
    }

    private fun startLoop() {
        if (job?.isActive == true) return
        startForegroundWithStatus("壁纸轮播已开启", "正在切换...")
        job = scope.launch {
            val startTime = System.currentTimeMillis()
            var i = 0
            var currentOrder: List<android.net.Uri>? = null
            var lastOrderType: PlaybackOrder? = null
            
            while (isActive) {
                // 每次循环迭代时重新读取配置
                val uris = selectionRepo.selectedFlow.first()
                val cfg = settingsRepo.configFlow.first()
                
                if (uris.isEmpty()) {
                    stopSelf()
                    return@launch
                }
                
                // 检测到播放顺序变化时，重新生成播放顺序列表
                val shouldRegenerate = currentOrder == null || 
                    lastOrderType != cfg.order || 
                    currentOrder.size != uris.size ||
                    currentOrder.toSet() != uris.toSet()
                
                if (shouldRegenerate) {
                    currentOrder = if (cfg.order == PlaybackOrder.RANDOM) {
                        uris.shuffled(Random(System.currentTimeMillis()))
                    } else {
                        uris
                    }
                    lastOrderType = cfg.order
                    // 重置索引，从新列表的第一张开始
                    i = 0
                }
                
                val uri = currentOrder!![i % currentOrder!!.size]
                applyUri(uri, cfg.applyScope)
                
                val elapsed = System.currentTimeMillis() - startTime
                if (cfg.maxDurationMs != null && elapsed >= cfg.maxDurationMs) {
                    break
                }
                
                delay(cfg.perItemMs)
                i++
            }
            stopSelf()
        }
    }

    private fun toggle() {
        if (job?.isActive == true) {
            stopSelf()
        } else {
            startLoop()
        }
    }

    private fun startForegroundWithStatus(title: String, text: String) {
        val notif = Notifications.ongoing(
            this, title, text, actions = listOf(
                action(ACTION_PREV, "上一张"),
                action(ACTION_TOGGLE, "暂停/继续"),
                action(ACTION_NEXT, "下一张"),
                action(ACTION_STOP, "停止")
            )
        )
        startForeground(Notifications.NOTIFICATION_ID, notif)
    }

    private fun action(action: String, title: String): NotificationCompat.Action {
        val pi = PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, SlideshowService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, title, pi).build()
    }

    private fun applyNextOne() {
        scope.launch { step(1) }
    }

    private fun applyPrevOne() {
        scope.launch { step(-1) }
    }

    private suspend fun step(delta: Int) {
        val uris = selectionRepo.selectedFlow.first()
        val cfg = settingsRepo.configFlow.first()
        if (uris.isEmpty()) return
        // 每次手动切换时重新读取配置并生成顺序列表
        val list =
            if (cfg.order == PlaybackOrder.RANDOM) uris.shuffled(Random(System.currentTimeMillis())) else uris
        val index = if (delta >= 0) 1 else list.size - 1
        val uri = list[index % list.size]
        applyUri(uri, cfg.applyScope)
    }

    private suspend fun applyUri(uri: android.net.Uri, scope: ApplyScope) {
        applier.setFromUri(uri, scope)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.pixlwallo.action.START"
        const val ACTION_STOP = "com.example.pixlwallo.action.STOP"
        const val ACTION_NEXT = "com.example.pixlwallo.action.NEXT"
        const val ACTION_PREV = "com.example.pixlwallo.action.PREV"
        const val ACTION_TOGGLE = "com.example.pixlwallo.action.TOGGLE"

        fun start(context: Context) {
            val i = Intent(context, SlideshowService::class.java).setAction(ACTION_START)
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            val i = Intent(context, SlideshowService::class.java).setAction(ACTION_STOP)
            context.startService(i)
        }
    }
}