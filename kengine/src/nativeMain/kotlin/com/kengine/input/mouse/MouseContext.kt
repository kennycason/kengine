package com.kengine.input.mouse

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class MouseContext private constructor(
    val mouse: MouseInputEventSubscriber
) : Context(), Logging {

    // Track button states
    private var wasLeftPressed: Boolean = false
    private var isLeftPressed: Boolean = false

    override fun cleanup() {
        logger.debug { "Cleaning up MouseContext" }
        currentContext = null
    }

    fun wasLeftPressed(): Boolean {
        return wasLeftPressed
    }

    fun wasLeftReleased(): Boolean {
        return wasLeftPressed && !isLeftPressed
    }

    fun init() {
        if (logger.isTraceEnabled()) {
            logger.trace { "Initializing MouseContext" }
        }
        mouse.init()
    }

    companion object {
        private var currentContext: MouseContext? = null

        fun get(): MouseContext {
            return currentContext ?: MouseContext(
                mouse = MouseInputEventSubscriber()
            ).also {
                currentContext = it
            }
        }
    }
}
