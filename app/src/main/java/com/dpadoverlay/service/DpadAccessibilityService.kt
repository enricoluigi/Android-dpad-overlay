package com.dpadoverlay.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
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
        return KeyInjector.injectKey(keyCode)
    }

    companion object {
        @Volatile
        private var instance: DpadAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = manager.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
            )
            val component = ComponentName(context, DpadAccessibilityService::class.java)
            return enabledServices.any { it.resolveInfo.serviceInfo.let { info ->
                info.packageName == component.packageName && info.name == component.className
            } }
        }

        fun sendKey(keyCode: Int): Boolean {
            return instance?.sendKey(keyCode) ?: false
        }
    }
}
