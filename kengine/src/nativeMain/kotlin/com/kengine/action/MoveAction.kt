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
    val onComplete: (() -> Unit)? = null
) : Action, Logging {
    private val clock = getContext<GameContext>().clock
    private val startTimeMs = clock.totalTimeMs
    private val startPoint = entity.p.copy()

    override fun update(): Boolean {
        val elapsedMs = clock.totalTimeMs - startTimeMs // Use global clock for consistency
        val frameTime = clock.deltaTimeMs // Access frame timing for drift checks

        // allow minor drift to handle sub-frame precision
        if (elapsedMs + frameTime >= durationMs) {
            logger.warn { "expiring move action: ${entity::class.simpleName} after ${elapsedMs}ms. Current time: ${clock.totalTimeMs}" }
            entity.p.set(destination) // Snap to destination for precision
            onComplete?.invoke()
            return true
        }

        // calculate progress and interpolate position
        val progress = (elapsedMs / durationMs.toDouble()).coerceIn(0.0, 1.0)
        entity.p.set(startPoint.linearInterpolate(destination, progress))

        // predicted remaining time and log output
        val predictedRemaining = durationMs - elapsedMs - frameTime
        logger.debug {
            "elapsed: ${elapsedMs + frameTime} ms | progress: ${(progress * 100).toInt()}% | predicted remaining: $predictedRemaining ms"
        }

        return false
    }
}
