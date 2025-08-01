package com.kengine.hooks.state

inline fun <reified T> useState(initialValue: T): State<T> {
    return State(initialValue)
}