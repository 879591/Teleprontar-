package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class FloatingTeleprompterService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var scriptTitle = "Floating Teleprompter"
    private var scriptContent = ""
    private var scrollSpeed = 130 // wpm
    private var isPlaying = false
    private var currentScrollY = 0

    private val scrollHandler = Handler(Looper.getMainLooper())
    private var scrollRunnable: Runnable? = null

    companion object {
        const val CHANNEL_ID = "FloatingTeleprompterChannel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_SPEED = "extra_speed"
        const val ACTION_STOP = "action_stop"
        var isServiceRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        scriptTitle = intent?.getStringExtra(EXTRA_TITLE) ?: "Teleprompter"
        scriptContent = intent?.getStringExtra(EXTRA_CONTENT) ?: ""
        scrollSpeed = intent?.getIntExtra(EXTRA_SPEED, 130) ?: 130

        if (Settings.canDrawOverlays(this)) {
            showFloatingWindow()
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    private fun showFloatingWindow() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val initialWidth = (displayMetrics.widthPixels * 0.88).toInt().coerceAtMost(1000)
        val initialHeight = (displayMetrics.heightPixels * 0.40).toInt().coerceAtMost(800)

        params = WindowManager.LayoutParams(
            initialWidth,
            initialHeight,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (displayMetrics.widthPixels - initialWidth) / 2
            y = 120
        }

        floatingView = createFloatingLayout()
        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun createFloatingLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.argb(220, 15, 23, 42)) // dark studio glass
                cornerRadius = 32f
                setStroke(3, Color.parseColor("#00E5FF")) // cyan border
            }
            background = bg
            setPadding(16, 12, 16, 16)
        }

        // Header with Drag Handle, Title & Buttons
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 4, 8, 8)
        }

        val titleTv = TextView(this).apply {
            text = scriptTitle
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
        }

        val openAppBtn = TextView(this).apply {
            text = "App ↗"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 12f
            setPadding(12, 6, 12, 6)
            setOnClickListener {
                val launchIntent = Intent(this@FloatingTeleprompterService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(launchIntent)
            }
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 18f
            setPadding(16, 4, 8, 4)
            setOnClickListener {
                stopSelf()
            }
        }

        header.addView(titleTv)
        header.addView(openAppBtn)
        header.addView(closeBtn)

        // Drag listener on header
        header.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params?.let { p ->
                            p.x = initialX + (event.rawX - initialTouchX).toInt()
                            p.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(floatingView, p)
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Content Scroll View
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isVerticalScrollBarEnabled = true
        }

        val contentTv = TextView(this).apply {
            text = if (scriptContent.isNotBlank()) scriptContent else "कोई स्क्रिप्ट टेक्स्ट नहीं है। ऐप में नया स्क्रिप्ट जोड़ें।"
            setTextColor(Color.WHITE)
            textSize = 20f // Readable font size
            setLineSpacing(12f, 1.2f)
            setPadding(12, 16, 12, 16)
        }

        scrollView.addView(contentTv)

        // Bottom Controls Bar
        val controlsBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 4)
        }

        val playPauseBtn = Button(this).apply {
            text = "▶ Play"
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#00E5FF"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics).toInt()
            )
            setOnClickListener {
                isPlaying = !isPlaying
                text = if (isPlaying) "⏸ Pause" else "▶ Play"
                if (isPlaying) {
                    startAutoScroll(scrollView)
                } else {
                    stopAutoScroll()
                }
            }
        }

        val speedTv = TextView(this).apply {
            text = "  ${scrollSpeed}wpm  "
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
        }

        val slowerBtn = Button(this).apply {
            text = "-"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#334155"))
            textSize = 14f
            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener {
                scrollSpeed = (scrollSpeed - 15).coerceAtLeast(40)
                speedTv.text = "  ${scrollSpeed}wpm  "
            }
        }

        val fasterBtn = Button(this).apply {
            text = "+"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#334155"))
            textSize = 14f
            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener {
                scrollSpeed = (scrollSpeed + 15).coerceAtMost(350)
                speedTv.text = "  ${scrollSpeed}wpm  "
            }
        }

        val restartBtn = Button(this).apply {
            text = "↺"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#334155"))
            textSize = 14f
            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener {
                scrollView.scrollTo(0, 0)
                currentScrollY = 0
            }
        }

        controlsBar.addView(playPauseBtn)
        controlsBar.addView(slowerBtn)
        controlsBar.addView(speedTv)
        controlsBar.addView(fasterBtn)
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }
        controlsBar.addView(spacer)
        controlsBar.addView(restartBtn)

        root.addView(header)
        root.addView(scrollView)
        root.addView(controlsBar)

        return root
    }

    private fun startAutoScroll(scrollView: ScrollView) {
        stopAutoScroll()
        scrollRunnable = object : Runnable {
            override fun run() {
                if (!isPlaying) return
                // Convert wpm to pixels per tick (assuming 60 fps / approx 16ms tick)
                val pixelsPerTick = (scrollSpeed.toFloat() / 130f) * 1.5f
                currentScrollY = (currentScrollY + pixelsPerTick.toInt()).coerceAtLeast(scrollView.scrollY + 1)
                scrollView.scrollTo(0, currentScrollY)
                scrollHandler.postDelayed(this, 30)
            }
        }
        scrollHandler.post(scrollRunnable!!)
    }

    private fun stopAutoScroll() {
        scrollRunnable?.let { scrollHandler.removeCallbacks(it) }
        scrollRunnable = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Teleprompter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating teleprompter over other apps"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingTeleprompterService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Teleprompter Active")
            .setContentText("Tap to return to app or stop floating window")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Overlay", pendingStop)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopAutoScroll()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }
}
