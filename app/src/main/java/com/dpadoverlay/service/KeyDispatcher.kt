package com.dpadoverlay.service

import android.content.Context
import android.view.KeyEvent
import com.dpadoverlay.R

object KeyDispatcher {

    fun sendKey(context: Context, keyCode: Int): Boolean {
        if (KeyInjector.injectKey(context, keyCode)) {
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            return DpadAccessibilityService.sendKey(keyCode)
        }
        return false
    }

    fun failureMessageRes(context: Context, keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                when {
                    !ShizukuHelper.isInstalled(context) -> R.string.shizuku_install_required
                    !ShizukuHelper.isRunning() -> R.string.shizuku_start_required
                    !ShizukuHelper.hasPermission() -> R.string.shizuku_permission_required
                    else -> R.string.dpad_send_failed
                }
            }
            else -> R.string.key_send_failed
        }
    }
}
