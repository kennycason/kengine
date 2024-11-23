package com.kengine.input

import com.kengine.context.Context

class KeyboardContext private constructor(
    val keyboardInput: KeyboardInputSubscriber
) : Context() {

    companion object {
        private var currentContext: KeyboardContext? = null

        fun get(): KeyboardContext {
            if (currentContext == null) {
                currentContext = KeyboardContext(
                    keyboardInput = KeyboardInputSubscriber()
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create keyboard context")
        }
    }

    override fun cleanup() {
    }
}