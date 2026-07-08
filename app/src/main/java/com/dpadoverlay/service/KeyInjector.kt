package com.dpadoverlay.service

import android.os.SystemClock
import android.view.InputEvent
import android.view.KeyEvent

object KeyInjector {

    private const val INJECT_INPUT_EVENT_MODE_ASYNC = 0

    fun injectKey(keyCode: Int): Boolean {
        return injectViaReflection(keyCode) || injectViaShell(keyCode)
    }

    private fun injectViaReflection(keyCode: Int): Boolean {
        return try {
            val now = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(now, now + 50, KeyEvent.ACTION_UP, keyCode, 0)
            val inputManagerClass = Class.forName("android.hardware.input.InputManager")
            val getInstance = inputManagerClass.getDeclaredMethod("getInstance")
            val inputManager = getInstance.invoke(null)
            val injectMethod = inputManager.javaClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
            val downOk = injectMethod.invoke(inputManager, downEvent, INJECT_INPUT_EVENT_MODE_ASYNC) as Boolean
            val upOk = injectMethod.invoke(inputManager, upEvent, INJECT_INPUT_EVENT_MODE_ASYNC) as Boolean
            downOk && upOk
        } catch (_: Exception) {
            false
        }
    }

    private fun injectViaShell(keyCode: Int): Boolean {
        return try {
            val process = ProcessBuilder("input", "keyevent", keyCode.toString())
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }
}
