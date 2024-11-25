
package com.kengine.action

import com.kengine.time.getCurrentTimestampMilliseconds

data class TimerAction(
    val delayMs: Long,
    val onComplete: (() -> Unit)? = null
) : Action {
    private val startTimeMs = getCurrentTimestampMilliseconds()

    override fun update(deltaTime: Double): Boolean {
        if (getCurrentTimestampMilliseconds() - startTimeMs > delayMs) {
            onComplete?.invoke()
            return true
        }
        return false
    }
}
