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
import com.dpadoverlay.service.ShizukuHelper
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                com.dpadoverlay.service.ShizukuKeyInjector.bind(this@MainActivity)
            }
            updateUi()
        }

    private val shizukuBinderListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() = updateUi()
    }

    private val shizukuBinderDeadListener = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() = updateUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.addBinderReceivedListener(shizukuBinderListener)
        Shizuku.addBinderDeadListener(shizukuBinderDeadListener)

        if (ShizukuHelper.isReady()) {
            com.dpadoverlay.service.ShizukuKeyInjector.bind(this)
        }

        binding.btnOverlayPermission.setOnClickListener { requestOverlayPermission() }
        binding.btnShizuku.setOnClickListener { handleShizukuAction() }
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnToggleOverlay.setOnClickListener { toggleOverlay() }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.removeBinderReceivedListener(shizukuBinderListener)
        Shizuku.removeBinderDeadListener(shizukuBinderDeadListener)
        com.dpadoverlay.service.ShizukuKeyInjector.unbind()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (ShizukuHelper.isReady()) {
            com.dpadoverlay.service.ShizukuKeyInjector.bind(this)
        }
        updateUi()
    }

    private fun updateUi() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val shizukuInstalled = ShizukuHelper.isInstalled(this)
        val shizukuRunning = ShizukuHelper.isRunning()
        val shizukuReady = ShizukuHelper.isReady()
        val accessibilityEnabled = DpadAccessibilityService.isEnabled(this)
        val accessibilityConnected = DpadAccessibilityService.isConnected()
        val overlayRunning = OverlayService.isRunning

        binding.statusOverlayPermission.text = getString(
            if (overlayGranted) R.string.status_granted else R.string.status_denied
        )
        binding.statusShizuku.text = getString(
            when {
                shizukuReady -> R.string.status_ready
                shizukuRunning -> R.string.status_waiting_permission
                shizukuInstalled -> R.string.status_not_running
                else -> R.string.status_not_installed
            }
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

        binding.btnShizuku.text = getString(
            when {
                !shizukuInstalled -> R.string.install_shizuku
                !shizukuRunning -> R.string.open_shizuku
                !ShizukuHelper.hasPermission() -> R.string.grant_shizuku_permission
                else -> R.string.shizuku_ready_action
            }
        )

        val ready = overlayGranted && shizukuReady
        binding.btnToggleOverlay.isEnabled = ready
        binding.btnToggleOverlay.text = getString(
            if (overlayRunning) R.string.stop_overlay else R.string.start_overlay
        )
    }

    private fun handleShizukuAction() {
        when {
            !ShizukuHelper.isInstalled(this) -> openShizukuInstallPage()
            !ShizukuHelper.isRunning() -> openShizukuApp()
            !ShizukuHelper.hasPermission() -> ShizukuHelper.requestPermission()
            else -> Toast.makeText(this, R.string.shizuku_already_ready, Toast.LENGTH_SHORT).show()
        }
        binding.root.postDelayed({ updateUi() }, 300)
    }

    private fun openShizukuApp() {
        val launch = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (launch != null) {
            startActivity(launch)
        } else {
            openShizukuInstallPage()
        }
    }

    private fun openShizukuInstallPage() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://shizuku.rikka.app/download/")
            )
        )
    }

    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.overlay_already_granted, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun toggleOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
            return
        }
        if (!ShizukuHelper.isReady()) {
            Toast.makeText(this, R.string.shizuku_required_to_start, Toast.LENGTH_LONG).show()
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
