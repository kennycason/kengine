package com.kengine.hooks.effect

import com.kengine.hooks.state.State

data class Effect(
    private val effect: () -> Unit,
    private val dependencies: List<State<*>>
) {
    // map each dependency to its subscription callback, this is so we can cleanup/unsubscribe if needed.
    private val subscriptions = mutableMapOf<State<*>, (Any?) -> Unit>()

    fun execute() {
        // execute the effect initially
        effect()

        // subscribe to dependencies and store their callbacks
        dependencies.forEach { dependency ->
            val callback: (Any?) -> Unit = { _ -> effect() }
            dependency.subscribe(callback)
            subscriptions[dependency] = callback
        }
    }

    fun cleanup() {
        // unsubscribe each dependency's callback
        subscriptions.forEach { (dependency, callback) ->
            dependency.unsubscribe(callback)
        }
        subscriptions.clear()
    }
}