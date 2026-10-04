package com.example.service

import android.annotation.SuppressLint
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
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NinjaOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "ninja_floating_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ninja Floating Overlay Active",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ninja Stealth Widget Active")
            .setContentText("Tap to return to Ninja Habit Builder")
            .setSmallIcon(R.drawable.ninja_habit_icon_1791128794368)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(2002, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (overlayView == null) {
            showOverlayWidget()
        }

        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun showOverlayWidget() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.density * 280).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 160
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 28, 36, 28)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#EE0B0F19"))
                cornerRadius = 32f
                setStroke(3, Color.parseColor("#34D399"))
            }
            background = bg
            elevation = 24f
        }

        // Header with title and close button
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "🥷 STEALTH FOCUS"
            setTextColor(Color.parseColor("#34D399"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#9CA3AF"))
            textSize = 18f
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                stopSelf()
            }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(closeButton)
        container.addView(headerLayout)

        // Workout and tip content
        val contentText = TextView(this).apply {
            text = "Loading today's stealth exercise..."
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 16, 0, 16)
        }
        container.addView(contentText)

        val actionButton = Button(this).apply {
            text = "Open Ninja Builder"
            setTextColor(Color.parseColor("#064E3B"))
            textSize = 12f
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 20f
            }
            background = btnBg
            setOnClickListener {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(launchIntent)
            }
        }
        container.addView(actionButton)

        // Drag-to-move listener
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(container, params)
                        return true
                    }
                }
                return false
            }
        })

        overlayView = container

        try {
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return
        }

        // Load dynamic tip
        val repo = HabitRepository.getInstance(this)
        scope.launch {
            val day = repo.getCurrentDayNumberSync()
            val tip = repo.getTipForDay(day)
            contentText.text = "Day $day\n\n🥋 ${tip.exerciseTitle}:\n${tip.exerciseBody.take(120)}..."
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
    }

    companion object {
        fun startOverlay(context: Context) {
            val intent = Intent(context, NinjaOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, NinjaOverlayService::class.java)
            context.stopService(intent)
        }
    }
}
