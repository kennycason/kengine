package com.kengine.state

inline fun <reified T> useState(initialValue: T): State<T> {
    return State(initialValue)
}