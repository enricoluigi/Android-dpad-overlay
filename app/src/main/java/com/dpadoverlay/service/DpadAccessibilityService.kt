package com.dpadoverlay.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return performGlobalAction(GLOBAL_ACTION_BACK)
        }
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return performGlobalAction(GLOBAL_ACTION_HOME)
        }
        if (isDpadKey(keyCode)) {
            if (KeyInjector.injectKey(keyCode)) {
                return true
            }
            if (FocusNavigator.navigate(this, keyCode)) {
                return true
            }
            return GestureNavigator.scroll(this, keyCode)
        }
        return false
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
            val flattened = component.flattenToString()
            val shortFlattened = component.flattenToShortString()
            return enabled.split(':').any { entry ->
                entry.equals(flattened, ignoreCase = true) ||
                    entry.equals(shortFlattened, ignoreCase = true)
            }
        }

        fun isConnected(): Boolean = instance != null

        fun sendKey(keyCode: Int): Boolean {
            return instance?.sendKey(keyCode) ?: false
        }

        private fun isDpadKey(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        }
    }
}
