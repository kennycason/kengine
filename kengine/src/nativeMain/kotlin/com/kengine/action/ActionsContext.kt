package com.kengine.action

import com.kengine.context.Context
import com.kengine.entity.Entity
import com.kengine.math.Vec2

class ActionsContext: Context() {
    private val actions = ArrayDeque<Action>()

    fun moveTo(entity: Entity, destination: Vec2, speed: Double, onComplete: (() -> Unit)? = null) {
        actions.add(MoveAction(entity, destination, speed, onComplete))
    }

    fun timer(delayMs: Long, onComplete: (() -> Unit)? = null) {
        actions.add(TimerAction(delayMs, onComplete))
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
        actions.clear()
    }

    companion object {
        private val instance = ActionsContext()
        fun get(): ActionsContext = instance
    }
}