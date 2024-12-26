package com.kengine.input.keyboard

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class KeyboardContext private constructor(
    val keyboard: KeyboardInputEventSubscriber
) : Context(), Logging {

    companion object {
        private var currentContext: KeyboardContext? = null

        fun get(): KeyboardContext {
            if (currentContext == null) {
                currentContext = KeyboardContext(
                    keyboard = KeyboardInputEventSubscriber()
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create keyboard context")
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up KeyboardContext"}
        currentContext = null
    }

    fun init() {
        keyboard.init()
    }
}
