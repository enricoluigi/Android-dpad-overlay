package com.dpadoverlay.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.WindowManager

object GestureNavigator {

    fun scroll(service: AccessibilityService, keyCode: Int): Boolean {
        val direction = keyCodeToDirection(keyCode) ?: return false
        val metrics = service.displayMetrics()
        val centerX = metrics.widthPixels / 2f
        val centerY = metrics.heightPixels / 2f
        val delta = metrics.heightPixels * 0.12f

        val path = Path()
        when (direction) {
            Direction.UP -> {
                path.moveTo(centerX, centerY + delta)
                path.lineTo(centerX, centerY - delta)
            }
            Direction.DOWN -> {
                path.moveTo(centerX, centerY - delta)
                path.lineTo(centerX, centerY + delta)
            }
            Direction.LEFT -> {
                path.moveTo(centerX + delta, centerY)
                path.lineTo(centerX - delta, centerY)
            }
            Direction.RIGHT -> {
                path.moveTo(centerX - delta, centerY)
                path.lineTo(centerX + delta, centerY)
            }
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 120))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }

    private fun AccessibilityService.displayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WindowManager::class.java)
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private fun keyCodeToDirection(keyCode: Int): Direction? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> Direction.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> Direction.RIGHT
        else -> null
    }

    private enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
