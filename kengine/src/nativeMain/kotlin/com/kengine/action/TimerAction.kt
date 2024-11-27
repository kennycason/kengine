
package com.kengine.action

import com.kengine.GameContext

data class TimerAction(
    val delayMs: Long,
    val onComplete: (() -> Unit)? = null
) : Action {
    private val clock = GameContext.get().clock
    private val startTimeMs = clock.totalTimeMs

    override fun update(): Boolean {
        if (clock.totalTimeMs - startTimeMs > delayMs) {
            onComplete?.invoke()
            return true
        }
        return false
    }
}
