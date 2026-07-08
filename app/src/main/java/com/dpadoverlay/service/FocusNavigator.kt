package com.dpadoverlay.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo

object FocusNavigator {

    fun navigate(service: AccessibilityService, keyCode: Int): Boolean {
        val direction = keyCodeToDirection(keyCode) ?: return false
        val root = service.rootInActiveWindow ?: return false

        return try {
            val current = findCurrentNode(root)
            if (current != null) {
                try {
                    scrollContainer(current, direction) ||
                        navigateToSibling(current, direction) ||
                        navigateSpatially(current, root, direction)
                } finally {
                    current.recycle()
                }
            } else {
                navigateSpatiallyFromRoot(root, direction)
            }
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
        if (node.isFocused || node.isAccessibilityFocused) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstFocused(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun scrollContainer(from: AccessibilityNodeInfo, direction: Direction): Boolean {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(from)
        while (current != null) {
            val scrollAction = scrollActionFor(direction)
            if (scrollAction != null && hasAction(current, scrollAction)) {
                if (current.performAction(scrollAction)) {
                    current.recycle()
                    return true
                }
            }

            if (current.isScrollable) {
                val legacyAction = when (direction) {
                    Direction.UP, Direction.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    Direction.DOWN, Direction.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                }
                if (current.performAction(legacyAction)) {
                    current.recycle()
                    return true
                }
            }

            val parent = current.parent
            current.recycle()
            current = parent
        }
        return false
    }

    private fun scrollActionFor(direction: Direction): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return when (direction) {
            Direction.UP -> 0x00001000
            Direction.DOWN -> 0x00002000
            Direction.LEFT -> 0x00004000
            Direction.RIGHT -> 0x00008000
        }
    }

    private fun navigateToSibling(current: AccessibilityNodeInfo, direction: Direction): Boolean {
        val parent = current.parent ?: return false
        val currentRect = Rect()
        current.getBoundsInScreen(currentRect)

        var bestSibling: AccessibilityNodeInfo? = null
        var bestDistance = Int.MAX_VALUE

        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i) ?: continue
            if (sibling == current) {
                sibling.recycle()
                continue
            }
            if (!isFocusCandidate(sibling)) {
                sibling.recycle()
                continue
            }

            val siblingRect = Rect()
            sibling.getBoundsInScreen(siblingRect)
            if (siblingRect.isEmpty) {
                sibling.recycle()
                continue
            }

            val valid = when (direction) {
                Direction.UP -> siblingRect.bottom <= currentRect.top - 4
                Direction.DOWN -> siblingRect.top >= currentRect.bottom + 4
                Direction.LEFT -> siblingRect.right <= currentRect.left - 4
                Direction.RIGHT -> siblingRect.left >= currentRect.right + 4
            }

            if (!valid) {
                sibling.recycle()
                continue
            }

            val distance = when (direction) {
                Direction.UP -> currentRect.top - siblingRect.bottom
                Direction.DOWN -> siblingRect.top - currentRect.bottom
                Direction.LEFT -> currentRect.left - siblingRect.right
                Direction.RIGHT -> siblingRect.left - currentRect.right
            }

            if (distance in 0 until bestDistance) {
                bestSibling?.recycle()
                bestDistance = distance
                bestSibling = sibling
            } else {
                sibling.recycle()
            }
        }

        parent.recycle()

        if (bestSibling == null) return false
        val focused = focusNode(bestSibling)
        bestSibling.recycle()
        return focused
    }

    private fun navigateSpatially(
        current: AccessibilityNodeInfo,
        root: AccessibilityNodeInfo,
        direction: Direction
    ): Boolean {
        val currentRect = Rect()
        current.getBoundsInScreen(currentRect)
        if (currentRect.isEmpty) return false

        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectFocusCandidates(root, candidates)

        val best = findBestInDirection(currentRect, candidates, direction, current) ?: return false
        val focused = focusNode(best)
        best.recycle()
        return focused
    }

    private fun navigateSpatiallyFromRoot(root: AccessibilityNodeInfo, direction: Direction): Boolean {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectFocusCandidates(root, candidates)
        if (candidates.isEmpty()) return false

        val anchorRect = Rect()
        root.getBoundsInScreen(anchorRect)
        if (anchorRect.isEmpty) {
            candidates.forEach { it.recycle() }
            return false
        }

        val pivot = when (direction) {
            Direction.UP, Direction.DOWN -> Rect(
                anchorRect.centerX(),
                anchorRect.centerY(),
                anchorRect.centerX(),
                anchorRect.centerY()
            )
            Direction.LEFT, Direction.RIGHT -> Rect(
                anchorRect.centerX(),
                anchorRect.centerY(),
                anchorRect.centerX(),
                anchorRect.centerY()
            )
        }

        val best = findBestInDirection(pivot, candidates, direction, null) ?: return false
        val focused = focusNode(best)
        best.recycle()
        return focused
    }

    private fun collectFocusCandidates(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (isFocusCandidate(node)) {
            out.add(AccessibilityNodeInfo.obtain(node))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectFocusCandidates(child, out)
            child.recycle()
        }
    }

    private fun isFocusCandidate(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty || rect.width() < 8 || rect.height() < 8) return false
        if (node.isClickable && !node.isFocusable) return false
        return node.isFocusable ||
            hasAction(node, AccessibilityNodeInfo.ACTION_FOCUS) ||
            hasAction(node, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
    }

    private fun hasAction(node: AccessibilityNodeInfo, action: Int): Boolean {
        return node.actionList.any { it.id == action }
    }

    private fun findBestInDirection(
        current: Rect,
        candidates: List<AccessibilityNodeInfo>,
        direction: Direction,
        exclude: AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {
        val currentCenterX = current.centerX()
        val currentCenterY = current.centerY()
        var best: AccessibilityNodeInfo? = null
        var bestScore = Float.MAX_VALUE

        for (candidate in candidates) {
            if (exclude != null && candidate == exclude) continue

            val rect = Rect()
            candidate.getBoundsInScreen(rect)
            if (rect.isEmpty) continue

            val cx = rect.centerX()
            val cy = rect.centerY()

            val (primaryDistance, secondaryDistance, valid) = when (direction) {
                Direction.UP -> {
                    val dy = currentCenterY - cy
                    val dx = kotlin.math.abs(cx - currentCenterX)
                    Triple(dy.toFloat(), dx.toFloat(), dy > 8)
                }
                Direction.DOWN -> {
                    val dy = cy - currentCenterY
                    val dx = kotlin.math.abs(cx - currentCenterX)
                    Triple(dy.toFloat(), dx.toFloat(), dy > 8)
                }
                Direction.LEFT -> {
                    val dx = currentCenterX - cx
                    val dy = kotlin.math.abs(cy - currentCenterY)
                    Triple(dx.toFloat(), dy.toFloat(), dx > 8)
                }
                Direction.RIGHT -> {
                    val dx = cx - currentCenterX
                    val dy = kotlin.math.abs(cy - currentCenterY)
                    Triple(dx.toFloat(), dy.toFloat(), dx > 8)
                }
            }

            if (!valid || primaryDistance <= 0) continue

            val score = primaryDistance + secondaryDistance * 0.5f
            if (score < bestScore) {
                bestScore = score
                best?.recycle()
                best = AccessibilityNodeInfo.obtain(candidate)
            }
        }

        candidates.forEach { it.recycle() }
        return best
    }

    private fun focusNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        while (current != null) {
            if (hasAction(current, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)) {
                if (current.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)) {
                    recycleChain(current, node)
                    return true
                }
            }
            if (current.isFocusable && current.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                recycleChain(current, node)
                return true
            }
            val parent = current.parent
            if (current !== node) current.recycle()
            current = parent
        }
        return false
    }

    private fun recycleChain(from: AccessibilityNodeInfo, stopBefore: AccessibilityNodeInfo) {
        var current: AccessibilityNodeInfo? = from.parent
        while (current != null) {
            val parent = current.parent
            if (current !== stopBefore) current.recycle()
            current = parent
        }
    }

    private enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }

    fun activateFocused(service: AccessibilityService): Boolean {
        val root = service.rootInActiveWindow ?: return false
        return try {
            val focused = findCurrentNode(root) ?: return false
            try {
                clickFocusedNode(focused)
            } finally {
                focused.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    private fun clickFocusedNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        while (current != null) {
            if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                recycleChain(current, node)
                return true
            }
            val parent = current.parent
            if (current !== node) current.recycle()
            current = parent
        }
        return false
    }
}
