package com.kengine.input

import com.kengine.context.Context

class MouseContext private constructor(
    val mouse: MouseInputEventSubscriber
) : Context() {

    companion object {
        private var currentContext: MouseContext? = null

        fun get(): MouseContext {
            if (currentContext == null) {
                currentContext = MouseContext(
                    mouse = MouseInputEventSubscriber(),
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create mouse context")
        }
    }

    override fun cleanup() {
    }

    fun init() {
        mouse.init()
    }
}