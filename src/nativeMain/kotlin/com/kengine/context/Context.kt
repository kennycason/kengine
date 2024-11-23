package com.kengine.context

abstract class Context {
    private var isCreated: Boolean = false

    open fun create() {
        if (isCreated) {
            error("Context already created. Cleanup before re-creating.")
        }
        isCreated = true
    }

    open fun cleanup() {
        isCreated = false
    }

}