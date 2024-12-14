
package com.kengine.action

import com.kengine.GameContext
import com.kengine.hooks.context.getContext
import com.kengine.time.timeSinceMs

data class TimerAction(
    val delayMs: Long,
    val onComplete: (() -> Unit)? = null
) : Action {
    private val clock = getContext<GameContext>().clock
    private val startTimeMs = clock.totalTimeMs

    override fun update(): Boolean {
        if (timeSinceMs(startTimeMs) > delayMs) {
            onComplete?.invoke()
            return true
        }
        return false
    }
}
