package com.dpadoverlay.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.dpadoverlay.MainActivity
import com.dpadoverlay.R
import com.dpadoverlay.databinding.OverlayDpadBinding
import kotlin.math.abs

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayBinding: OverlayDpadBinding? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            startForegroundService()
            showOverlay()
            isRunning = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay", e)
            Toast.makeText(this, R.string.overlay_start_failed, Toast.LENGTH_LONG).show()
            isRunning = false
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        isRunning = false
        super.onDestroy()
    }

    private fun startForegroundService() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_overlay)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val themedContext = ContextThemeWrapper(this, R.style.Theme_DpadOverlay)
        val binding = OverlayDpadBinding.inflate(LayoutInflater.from(themedContext))
        overlayBinding = binding
        overlayView = binding.root

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 48
            y = 200
        }

        setupDrag(binding.dragHandle, params)
        setupKeyButtons(binding)

        windowManager?.addView(binding.root, params)
        binding.root.tag = params
    }

    private fun setupDrag(handle: View, params: WindowManager.LayoutParams) {
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (abs(deltaX) > DRAG_THRESHOLD || abs(deltaY) > DRAG_THRESHOLD) {
                        isDragging = true
                    }
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    overlayView?.let { windowManager?.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging
                else -> false
            }
        }
    }

    private fun setupKeyButtons(binding: OverlayDpadBinding) {
        binding.btnUp.setOnClickListener { sendKey(KeyEvent.KEYCODE_DPAD_UP) }
        binding.btnDown.setOnClickListener { sendKey(KeyEvent.KEYCODE_DPAD_DOWN) }
        binding.btnLeft.setOnClickListener { sendKey(KeyEvent.KEYCODE_DPAD_LEFT) }
        binding.btnRight.setOnClickListener { sendKey(KeyEvent.KEYCODE_DPAD_RIGHT) }
        binding.btnBack.setOnClickListener { sendKey(KeyEvent.KEYCODE_BACK) }
        binding.btnHome.setOnClickListener { sendKey(KeyEvent.KEYCODE_HOME) }
        binding.btnClose.setOnClickListener { stopSelf() }
    }

    private fun sendKey(keyCode: Int) {
        if (!DpadAccessibilityService.sendKey(keyCode)) {
            Toast.makeText(applicationContext, R.string.key_send_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Overlay view already removed", e)
            }
        }
        overlayView = null
        overlayBinding = null
        windowManager = null
    }

    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "dpad_overlay"
        private const val NOTIFICATION_ID = 1
        private const val DRAG_THRESHOLD = 8

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
