package com.marksameh.remote

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class RemoteAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        @Volatile private var active: RemoteAccessibilityService? = null
        fun performRemoteGesture(x: Double, y: Double, endX: Double, endY: Double, duration: Long) {
            val service = active ?: return
            val metrics = service.resources.displayMetrics
            val sx = (x.coerceIn(0.0, 1.0) * metrics.widthPixels).toFloat()
            val sy = (y.coerceIn(0.0, 1.0) * metrics.heightPixels).toFloat()
            val ex = (endX.coerceIn(0.0, 1.0) * metrics.widthPixels).toFloat()
            val ey = (endY.coerceIn(0.0, 1.0) * metrics.heightPixels).toFloat()
            val path = Path().apply { moveTo(sx, sy); lineTo(ex, ey) }
            val stroke = GestureDescription.StrokeDescription(path, 0, duration.coerceIn(50, 1500))
            service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        active = this
    }
    override fun onDestroy() {
        active = null
        super.onDestroy()
    }
}
