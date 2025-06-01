package com.kengine.action

import com.kengine.entity.Entity
import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import com.kengine.math.Vec2

class ActionContext private constructor() : Context(), Logging {

    private val actions = mutableListOf<Action>() // Current actions
    private val pendingActions = mutableListOf<Action>() // Actions added during update

    fun moveTo(entity: Entity, destination: Vec2, durationMs: Long, onComplete: (() -> Unit)) {
        pendingActions.add(MoveAction(entity, destination, durationMs, onComplete))
    }

    fun timer(delayMs: Long, onComplete: (() -> Unit)) {
        pendingActions.add(TimerAction(delayMs, onComplete))
    }

    fun interval(delayMs: Long, onComplete: (() -> Unit)) {
        pendingActions.add(IntervalAction(delayMs, onComplete))
    }

    fun update() {
        // early exit if no actions
        if (actions.isEmpty() && pendingActions.isEmpty()) return

        // process current actions
        // Iterate backwards to safely remove elements during iteration
        var i = actions.size - 1
        while (i >= 0) {
            val action = actions[i]
            if (action.update()) {
                // remove completed action
                actions.removeAt(i)
            }
            i--
        }

        // add pending actions to the main list
        if (pendingActions.isNotEmpty()) {
            actions.addAll(pendingActions)
            pendingActions.clear()
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up ActionContext" }
        actions.clear()
        pendingActions.clear()
    }

    fun size() = actions.size

    companion object {
        private val instance = ActionContext()
        fun get(): ActionContext = instance
    }
}
