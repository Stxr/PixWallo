package com.example.pixlwallo.dream

import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.LinearLayout
import android.widget.TextView
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.ViewGroup
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.pixlwallo.data.SelectionRepository
import com.example.pixlwallo.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PhotoDreamService : DreamService() {
    private lateinit var imageView: ImageView
    private lateinit var timeTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var selectionRepo: SelectionRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var loader: ImageLoader
    private var scope: CoroutineScope? = null
    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTimeAndDate()
            timeHandler.postDelayed(this, 1000) // 每秒更新一次
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        isInteractive = false
        setScreenBright(true)

        // 创建主容器
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
        }

        // 创建图片视图
        imageView = ImageView(this).apply {
            scaleType = ScaleType.CENTER_CROP
        }
        rootLayout.addView(
            imageView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 创建半透明遮罩层，确保文字可读
        val overlayView = ImageView(this).apply {
            setBackgroundColor(0x40000000.toInt()) // 半透明黑色遮罩
        }
        rootLayout.addView(
            overlayView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 创建时间日期容器（StandBy风格）
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 200) // 底部留白
        }

        // 时间显示（大字体）
        timeTextView = TextView(this).apply {
            textSize = 72f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        // 日期显示（中等字体）
        dateTextView = TextView(this).apply {
            textSize = 24f
            setTextColor(0xCCFFFFFF.toInt())
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            gravity = Gravity.CENTER
        }

        textContainer.addView(
            timeTextView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        textContainer.addView(
            dateTextView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        rootLayout.addView(
            textContainer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        )

        setContentView(rootLayout)
        selectionRepo = SelectionRepository(applicationContext)
        settingsRepo = SettingsRepository(applicationContext)
        loader = ImageLoader(this)
        
        // 立即更新时间
        updateTimeAndDate()
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        // 开始更新时间
        timeHandler.post(timeUpdateRunnable)
        
        val job = Job()
        scope = CoroutineScope(Dispatchers.Default + job)
        scope?.launch {
            val uris = selectionRepo.selectedFlow.first()
            if (uris.isEmpty()) return@launch
            val cfg = settingsRepo.configFlow.first()
            val list = if (cfg.order.name == "RANDOM") uris.shuffled(Random(System.currentTimeMillis())) else uris
            
            var i = 0
            while (isActive) {
                val uri = list[i % list.size]
                // 不指定固定尺寸，让 ImageView 的 CENTER_CROP 来处理裁剪
                // 这样可以保持图片的原始宽高比，不会被拉伸
                val req = ImageRequest.Builder(this@PhotoDreamService)
                    .data(uri)
                    .target(imageView)
                    .build()
                loader.enqueue(req)
                delay(cfg.perItemMs)
                i++
            }
        }
    }

    override fun onDreamingStopped() {
        // 停止更新时间
        timeHandler.removeCallbacks(timeUpdateRunnable)
        scope?.cancel()
        scope = null
        super.onDreamingStopped()
    }

    private fun updateTimeAndDate() {
        val calendar = Calendar.getInstance()
        
        // 更新时间显示
        val timeFormat = if (DateFormat.is24HourFormat(this)) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        }
        timeTextView.text = timeFormat.format(calendar.time)
        
        // 更新日期显示
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        dateTextView.text = dateFormat.format(calendar.time)
    }
}