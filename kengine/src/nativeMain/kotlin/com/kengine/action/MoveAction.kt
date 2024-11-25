
package com.kengine.action

import com.kengine.Vec2
import com.kengine.entity.Entity
import com.kengine.log.Logger
import com.kengine.time.getCurrentTimestampMilliseconds

data class MoveAction(
    val entity: Entity,
    val destination: Vec2,
    val speed: Double,
    val onComplete: (() -> Unit)? = null
) : Action {
    private val startTimeMs = getCurrentTimestampMilliseconds()
    private val expireIn = 5_000L

    override fun update(deltaTime: Double): Boolean {
        if (getCurrentTimestampMilliseconds() - startTimeMs > expireIn) {
            Logger.warn { "expiring move action: ${entity::class.simpleName} to $deltaTime" }
            return true
        }

        // calculate direction vector and distance to destination
        val direction = destination - entity.p
        val distance = direction.magnitude()

//        Logger.info { "move: $distance" }

        if (distance > 0.1) { // continue moving if not close enough
            val moveDistance = (speed * deltaTime).coerceAtMost(distance)
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
