package com.dpadoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dpadoverlay.databinding.ActivityMainBinding
import com.dpadoverlay.service.DpadAccessibilityService
import com.dpadoverlay.service.OverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOverlayPermission.setOnClickListener { requestOverlayPermission() }
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnToggleOverlay.setOnClickListener { toggleOverlay() }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val accessibilityEnabled = DpadAccessibilityService.isEnabled(this)
        val accessibilityConnected = DpadAccessibilityService.isConnected()
        val overlayRunning = OverlayService.isRunning

        binding.statusOverlayPermission.text = getString(
            if (overlayGranted) R.string.status_granted else R.string.status_denied
        )
        binding.statusAccessibility.text = getString(
            if (accessibilityEnabled) R.string.status_enabled else R.string.status_disabled
        )
        binding.statusAccessibilityConnected.text = getString(
            if (accessibilityConnected) R.string.status_enabled else R.string.status_disabled
        )
        binding.statusOverlayRunning.text = getString(
            if (overlayRunning) R.string.status_running else R.string.status_stopped
        )

        val ready = overlayGranted && accessibilityEnabled && accessibilityConnected
        binding.btnToggleOverlay.isEnabled = ready
        binding.btnToggleOverlay.text = getString(
            if (overlayRunning) R.string.stop_overlay else R.string.start_overlay
        )
    }

    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.overlay_already_granted, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun toggleOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
            return
        }
        if (!DpadAccessibilityService.isEnabled(this)) {
            Toast.makeText(this, R.string.accessibility_required, Toast.LENGTH_LONG).show()
            return
        }
        if (!DpadAccessibilityService.isConnected()) {
            Toast.makeText(this, R.string.accessibility_not_connected, Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }

        val intent = Intent(this, OverlayService::class.java)
        if (OverlayService.isRunning) {
            stopService(intent)
            Toast.makeText(this, R.string.overlay_stopped, Toast.LENGTH_SHORT).show()
        } else {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this, R.string.overlay_start_failed, Toast.LENGTH_LONG).show()
                return
            }
            Toast.makeText(this, R.string.overlay_started, Toast.LENGTH_SHORT).show()
        }
        binding.root.postDelayed({ updateUi() }, 300)
    }
}
