package com.dpadoverlay.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo

object FocusNavigator {

    fun navigate(service: AccessibilityService, keyCode: Int): Boolean {
        val direction = keyCodeToDirection(keyCode) ?: return false
        val root = service.rootInActiveWindow ?: return false

        return try {
            val current = findCurrentNode(root) ?: root
            val currentRect = Rect()
            current.getBoundsInScreen(currentRect)
            if (currentRect.isEmpty) return false

            val candidates = mutableListOf<AccessibilityNodeInfo>()
            collectActionableNodes(root, candidates)

            val best = findBestInDirection(currentRect, candidates, direction) ?: return false
            activateNode(best)
        } finally {
            root.recycle()
        }
    }

    private fun keyCodeToDirection(keyCode: Int): Direction? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> Direction.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> Direction.RIGHT
        else -> null
    }

    private fun findCurrentNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { return it }
        root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.let { return it }
        return findFirstFocused(root)
    }

    private fun findFirstFocused(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused || node.isAccessibilityFocused || node.isSelected) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstFocused(child)
            if (found != null) {
                if (found !== child) child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun collectActionableNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (isActionable(node)) {
            out.add(AccessibilityNodeInfo.obtain(node))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectActionableNodes(child, out)
            child.recycle()
        }
    }

    private fun isActionable(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty || rect.width() < 8 || rect.height() < 8) return false
        return node.isClickable || node.isFocusable || node.isCheckable ||
            node.actionList.any {
                it.id == AccessibilityNodeInfo.ACTION_CLICK ||
                    it.id == AccessibilityNodeInfo.ACTION_FOCUS ||
                    it.id == AccessibilityNodeInfo.ACTION_SELECT
            }
    }

    private fun findBestInDirection(
        current: Rect,
        candidates: List<AccessibilityNodeInfo>,
        direction: Direction
    ): AccessibilityNodeInfo? {
        val currentCenterX = current.centerX()
        val currentCenterY = current.centerY()
        var best: AccessibilityNodeInfo? = null
        var bestScore = Float.MAX_VALUE

        for (candidate in candidates) {
            val rect = Rect()
            candidate.getBoundsInScreen(rect)
            if (rect.isEmpty) continue

            val cx = rect.centerX()
            val cy = rect.centerY()

            val (primaryDistance, secondaryDistance, valid) = when (direction) {
                Direction.UP -> {
                    val dy = currentCenterY - cy
                    val dx = kotlin.math.abs(cx - currentCenterX)
                    Triple(dy.toFloat(), dx.toFloat(), dy > 12)
                }
                Direction.DOWN -> {
                    val dy = cy - currentCenterY
                    val dx = kotlin.math.abs(cx - currentCenterX)
                    Triple(dy.toFloat(), dx.toFloat(), dy > 12)
                }
                Direction.LEFT -> {
                    val dx = currentCenterX - cx
                    val dy = kotlin.math.abs(cy - currentCenterY)
                    Triple(dx.toFloat(), dy.toFloat(), dx > 12)
                }
                Direction.RIGHT -> {
                    val dx = cx - currentCenterX
                    val dy = kotlin.math.abs(cy - currentCenterY)
                    Triple(dx.toFloat(), dy.toFloat(), dx > 12)
                }
            }

            if (!valid || primaryDistance <= 0) continue

            val score = primaryDistance + secondaryDistance * 0.35f
            if (score < bestScore) {
                bestScore = score
                best?.recycle()
                best = AccessibilityNodeInfo.obtain(candidate)
            }
        }

        candidates.forEach { it.recycle() }
        return best
    }

    private fun activateNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        while (current != null) {
            if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                current.recycle()
                return true
            }
            if (current.isFocusable && current.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                current.recycle()
                return true
            }
            if (current.isCheckable && current.performAction(AccessibilityNodeInfo.ACTION_SELECT)) {
                current.recycle()
                return true
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return false
    }

    private enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
