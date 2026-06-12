package com.kengine.action

import com.kengine.GameContext
import com.kengine.entity.Entity
import com.kengine.hooks.context.getContext
import com.kengine.log.Logging
import com.kengine.math.Vec2

data class MoveAction(
    val entity: Entity,
    val destination: Vec2,
    val durationMs: Long,
    val easing: EasingFunction = Easing.linear,
    val onComplete: (() -> Unit)? = null
) : Action, Logging {
    private val clock = getContext<GameContext>().clock
    private val startTimeMs = clock.totalTimeMs
    private val startPoint = entity.p.copy()

    override fun update(): Boolean {
        val elapsedMs = clock.totalTimeMs - startTimeMs
        val frameTime = clock.deltaTimeMs

        if (elapsedMs + frameTime >= durationMs) {
            entity.p.set(destination)
            onComplete?.invoke()
            return true
        }

        val t = (elapsedMs / durationMs.toDouble()).coerceIn(0.0, 1.0)
        val easedT = easing(t)
        entity.p.set(startPoint.linearInterpolate(destination, easedT))

        return false
    }
}
