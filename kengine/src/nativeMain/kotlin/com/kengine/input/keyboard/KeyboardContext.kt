package com.kengine.input.keyboard

import com.kengine.hooks.context.Context

class KeyboardContext private constructor(
    val keyboard: KeyboardInputEventSubscriber
) : Context() {

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
    }

    fun init() {
        keyboard.init()
    }
}