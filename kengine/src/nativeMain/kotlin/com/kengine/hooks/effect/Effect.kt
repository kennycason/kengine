package com.kengine.hooks.effect

import com.kengine.hooks.state.State

data class Effect(
    private val effect: () -> Unit,
    private val dependencies: List<State<*>>
) {
    private val subscriptions = mutableMapOf<State<*>, (Any?) -> Unit>()
    private val values = dependencies.map { it.get() }.toMutableList()

    fun execute() {
        effect()
        dependencies.forEachIndexed { index, dependency ->
            val callback: (Any?) -> Unit = {
                if (values[index] != dependency.get()) { // Only trigger if value changes
                    values[index] = dependency.get()
                    effect()
                }
            }
            dependency.subscribe(callback)
            subscriptions[dependency] = callback
        }
    }

    fun checkDependencies() {
        dependencies.forEachIndexed { index, dependency ->
            if (values[index] != dependency.get()) { // Detect changes
                values[index] = dependency.get()
                effect() // Trigger effect
            }
        }
    }

    fun cleanup() {
        subscriptions.forEach { (dependency, callback) ->
            dependency.unsubscribe(callback)
        }
        subscriptions.clear()
    }
}
