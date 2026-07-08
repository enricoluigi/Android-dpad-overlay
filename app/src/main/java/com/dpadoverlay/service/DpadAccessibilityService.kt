package com.dpadoverlay.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class DpadAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    fun sendKey(keyCode: Int): Boolean {
        return performGlobalAction(keyCodeToGlobalAction(keyCode))
    }

    private fun keyCodeToGlobalAction(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> GLOBAL_ACTION_BACK
            KeyEvent.KEYCODE_HOME -> GLOBAL_ACTION_HOME
            KeyEvent.KEYCODE_DPAD_UP -> globalDpadAction(GLOBAL_ACTION_DPAD_UP, 16)
            KeyEvent.KEYCODE_DPAD_DOWN -> globalDpadAction(GLOBAL_ACTION_DPAD_DOWN, 17)
            KeyEvent.KEYCODE_DPAD_LEFT -> globalDpadAction(GLOBAL_ACTION_DPAD_LEFT, 18)
            KeyEvent.KEYCODE_DPAD_RIGHT -> globalDpadAction(GLOBAL_ACTION_DPAD_RIGHT, 19)
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> globalDpadAction(GLOBAL_ACTION_DPAD_CENTER, 20)
            else -> GLOBAL_ACTION_BACK
        }
    }

    private fun globalDpadAction(api33Constant: Int, fallbackConstant: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            api33Constant
        } else {
            fallbackConstant
        }
    }

    companion object {
        @Volatile
        private var instance: DpadAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            if (Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                ) != 1
            ) {
                return false
            }

            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val component = ComponentName(context, DpadAccessibilityService::class.java)
            return enabled.split(':').any { entry ->
                entry.equals(component.flattenToString(), ignoreCase = true) ||
                    entry.equals(component.flattenToShortString(), ignoreCase = true)
            }
        }

        fun isConnected(): Boolean = instance != null

        fun supportsDpadKeys(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }

        fun sendKey(keyCode: Int): Boolean {
            return instance?.sendKey(keyCode) ?: false
        }
    }
}
