package com.kengine.input.mouse

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class MouseContext private constructor(
    val mouse: MouseInputEventSubscriber
) : Context(), Logging {

    companion object {
        private var currentContext: MouseContext? = null

        fun get(): MouseContext {
            return currentContext ?: MouseContext(
                mouse = MouseInputEventSubscriber(),
            ).also {
                currentContext = it
            }
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up MouseContext" }
        currentContext = null
    }

    fun init() {
        mouse.init()
    }
}
