package com.kengine.action

import com.kengine.time.getClockContext

class TweenAction(
    val from: Double,
    val to: Double,
    val durationMs: Long,
    val easing: EasingFunction = Easing.linear,
    val onUpdate: ((Double) -> Unit)? = null,
    val onComplete: (() -> Unit)? = null
) : Action {
    private val clock = getClockContext()
    private val startTimeMs = clock.totalTimeMs

    override fun update(): Boolean {
        val elapsed = clock.totalTimeMs - startTimeMs
        if (elapsed >= durationMs) {
            onUpdate?.invoke(to)
            onComplete?.invoke()
            return true
        }

        val t = (elapsed / durationMs.toDouble()).coerceIn(0.0, 1.0)
        val easedT = easing(t)
        val value = from + (to - from) * easedT
        onUpdate?.invoke(value)
        return false
    }
}
