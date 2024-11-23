package com.kengine.input

import com.kengine.context.Context

class MouseContext private constructor(
    val mouseInput: MouseInputSubscriber
) : Context() {

    companion object {
        private var currentContext: MouseContext? = null

        fun get(): MouseContext {
            if (currentContext == null) {
                currentContext = MouseContext(
                    mouseInput = MouseInputSubscriber(),
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create mouse context")
        }
    }

    override fun cleanup() {
    }
}