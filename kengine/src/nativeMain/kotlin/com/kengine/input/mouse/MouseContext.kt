package com.kengine.input.mouse

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class MouseContext private constructor(
    val mouse: MouseInputEventSubscriber
) : Context(), Logging {

    override fun cleanup() {
        logger.debug { "Cleaning up MouseContext" }
        currentContext = null
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
