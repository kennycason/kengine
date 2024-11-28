
package com.kengine.action

import com.kengine.GameContext
import com.kengine.context.getContext
import com.kengine.entity.Entity
import com.kengine.log.Logger
import com.kengine.math.Vec2
import com.kengine.time.timeSinceMs

data class MoveAction(
    val entity: Entity,
    val destination: Vec2,
    val speed: Double,
    val onComplete: (() -> Unit)? = null
) : Action {
    private val clock = getContext<GameContext>().clock
    private val startTimeMs = clock.totalTimeMs
    private val expireInMs = 5000L

    override fun update(): Boolean {
        if (timeSinceMs(startTimeMs) > expireInMs) {
            Logger.warn { "expiring move action: ${entity::class.simpleName} after ${expireInMs}ms" }
            return true
        }

        // calculate direction vector and distance to destination
        val direction = destination - entity.p
        val distance = direction.magnitude()

        if (distance > 0.1) { // continue moving if not close enough
            val moveDistance = (speed * clock.deltaTimeSec).coerceAtMost(distance)
            val normalizedDirection = direction.normalized()
            entity.p += normalizedDirection * moveDistance
        }

        // check if the entity has reached its destination
        val reachedDestination = distance <= 0.1
        if (reachedDestination) {
            entity.p.set(destination) // snap to the destination to avoid precision issues
            onComplete?.invoke()
        }

        return reachedDestination // return true if movement is complete
    }
}
