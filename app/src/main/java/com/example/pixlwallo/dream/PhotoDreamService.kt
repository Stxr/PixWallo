package com.example.pixlwallo.dream

import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.ViewGroup
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.pixlwallo.data.SelectionRepository
import com.example.pixlwallo.data.SettingsRepository
import com.example.pixlwallo.model.ImgDisplayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        // 强制横屏 - 使用 SENSOR_LANDSCAPE 允许正反横屏，但禁止竖屏
        updateOrientation()

        // 创建主容器
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
        }

        // 创建图片视图
        imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
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
        // 再次确认横屏设置
        updateOrientation()

        // 开始更新时间
        timeHandler.post(timeUpdateRunnable)

        val job = Job()
        scope = CoroutineScope(Dispatchers.Default + job)
        scope?.launch {
            var currentOrder: List<android.net.Uri>? = null
            var lastOrderType: com.example.pixlwallo.model.PlaybackOrder? = null
            var i = 0

            while (isActive) {
                // 每次循环迭代时重新读取配置
                val uris = selectionRepo.selectedFlow.first()
                if (uris.isEmpty()) {
                    delay(1000) // 如果列表为空，等待1秒后重试
                    continue
                }

                val cfg = settingsRepo.configFlow.first()

                // 检测到播放顺序变化时，重新生成播放顺序列表
                val shouldRegenerate = currentOrder == null ||
                        lastOrderType?.name != cfg.order.name ||
                        currentOrder.size != uris.size ||
                        currentOrder.toSet() != uris.toSet()

                if (shouldRegenerate) {
                    currentOrder = if (cfg.order.name == "RANDOM") {
                        uris.shuffled(Random(System.currentTimeMillis()))
                    } else {
                        uris
                    }
                    lastOrderType = cfg.order
                    // 重置索引，从新列表的第一张开始
                    i = 0
                }

                val uri = currentOrder!![i % currentOrder!!.size]

                // 加载图片并获取尺寸
                val request = ImageRequest.Builder(this@PhotoDreamService)
                    .data(uri)
                    .build()

                val result = loader.execute(request)

                if (result is SuccessResult) {
                    val drawable = result.drawable
                    val isPortrait = drawable.intrinsicHeight > drawable.intrinsicWidth


                    withContext(Dispatchers.Main) {
                        // 根据图片方向和配置选择合适的 ScaleType
                        if (isPortrait) {
                            // 竖屏照片
                            when (cfg.imgDisplayMode) {
                                ImgDisplayMode.FILL_SCREEN -> {
                                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                                    imageView.rotation = 0f
                                }

                                ImgDisplayMode.FIT_CENTER -> {
                                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                                    imageView.rotation = 0f
                                }

                                ImgDisplayMode.SMART -> {
                                    // 智能模式：旋转90度，并交换宽高以正确填满屏幕
                                    // 注意：这里需要将 ImageView 的尺寸调整为反向匹配屏幕，然后旋转
                                    // 因为 rotation 是围绕中心旋转的，如果宽高不一致，旋转后可能不会填满
                                    // 但简单的做法是直接旋转，然后用 CENTER_CROP，
                                    // 不过如果 View 本身是宽>高（屏幕形状），旋转90度后，View 的内容区域是竖长的
                                    // 这时 CENTER_CROP 会把竖屏图片填满这个竖长区域
                                    // 视觉上就是：竖屏图片横躺在屏幕上
                                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                                    imageView.rotation = 90f
                                }
                            }
                        } else {
                            // 横屏照片：始终填满屏幕
                            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                            imageView.rotation = 0f
                        }
                        imageView.setImageDrawable(drawable)
                    }
                }

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

    private fun updateOrientation() {
        val params = window.attributes
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.attributes = params
    }
}