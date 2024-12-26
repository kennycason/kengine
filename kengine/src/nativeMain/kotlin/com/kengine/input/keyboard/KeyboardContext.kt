package com.kengine.input.keyboard

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class KeyboardContext private constructor(
    val keyboard: KeyboardInputEventSubscriber
) : Context(), Logging {

    companion object {
        private var currentContext: KeyboardContext? = null

        fun get(): KeyboardContext {
            return currentContext ?: KeyboardContext(
                keyboard = KeyboardInputEventSubscriber()
            ).also {
                currentContext = it
            }
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up KeyboardContext" }
        currentContext = null
    }

    fun init() {
        keyboard.init()
    }
}
