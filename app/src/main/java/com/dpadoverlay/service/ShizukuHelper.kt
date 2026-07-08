package com.dpadoverlay.service

import android.content.Context
import android.content.pm.PackageManager
import android.view.KeyEvent
import com.dpadoverlay.R
import rikka.shizuku.Shizuku

object ShizukuHelper {

    const val REQUEST_CODE = 1001
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun isReady(): Boolean = isRunning() && hasPermission()

    fun requestPermission() {
        if (isRunning() && !hasPermission()) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }
}
