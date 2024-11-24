package com.kengine.action

import com.kengine.Vec2
import com.kengine.context.Context
import com.kengine.entity.Entity

class ActionsContext: Context() {
    private val actions = ArrayDeque<Action>()

    fun moveTo(entity: Entity, destination: Vec2, speed: Double, onComplete: (() -> Unit)? = null) {
        actions.add(MoveAction(entity, destination, speed, onComplete))
    }

    fun update(deltaTime: Double) {
        val iterator = actions.iterator()
        while (iterator.hasNext()) {
            val action = iterator.next()
            if (action.update(deltaTime)) {
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