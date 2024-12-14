package com.kengine.memo

import com.kengine.hooks.state.State

data class MemoizedValue<T>(
    private val compute: () -> T,
    private val dependencies: List<State<*>>,
) {
    private var value: T? = null
    private val subscriptions = mutableMapOf<State<*>, (Any?) -> Unit>()

    fun get(): T {
        if (value == null) {
            calculate() // initialize on first access
        }
        return value!!
    }

    private fun calculate() {
        // unsubscribe from existing subscriptions to avoid duplicates
        cleanup()

        // compute the value
        value = compute()

        // subscribe to dependencies
        dependencies.forEach { dependency ->
            val callback: (Any?) -> Unit = { _ -> calculate() }
            dependency.subscribe(callback)
            subscriptions[dependency] = callback
        }
    }

    fun cleanup() {
        // unsubscribe all callbacks
        subscriptions.forEach { (dependency, callback) ->
            dependency.unsubscribe(callback)
        }
        subscriptions.clear()
    }
}
