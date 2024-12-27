package com.kengine.action

import com.kengine.entity.Entity
import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import com.kengine.math.Vec2

class ActionContext private constructor() : Context(), Logging {
    private val actions = ArrayDeque<Action>()

    fun moveTo(entity: Entity, destination: Vec2, speed: Double, onComplete: (() -> Unit)) {
        actions.add(MoveAction(entity, destination, speed, onComplete))
    }

    fun timer(delayMs: Long, onComplete: (() -> Unit)) {
        actions.add(TimerAction(delayMs, onComplete))
    }

    fun interval(delayMs: Long, onComplete: (() -> Unit)) {
        actions.add(IntervalAction(delayMs, onComplete))
    }

    fun update() {
        val iterator = actions.iterator()
        while (iterator.hasNext()) {
            val action = iterator.next()
            if (action.update()) {
                iterator.remove()
            }
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up ActionContext"}
        actions.clear()
    }

    fun size() = actions.size

    companion object {
        private val instance = ActionContext()
        fun get(): ActionContext = instance
    }
}
