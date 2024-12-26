package com.kengine.input.mouse

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class MouseContext private constructor(
    val mouse: MouseInputEventSubscriber
) : Context(), Logging {

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
        logger.info { "Cleaning up MouseContext"}
        currentContext = null
    }

    fun init() {
        mouse.init()
    }
}
