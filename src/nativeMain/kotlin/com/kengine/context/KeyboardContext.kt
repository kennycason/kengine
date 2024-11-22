package com.kengine.context

import com.kengine.input.KeyboardInput

class KeyboardContext private constructor(
    val keyboardInput: KeyboardInput
) : Context() {

    companion object {
        private var currentContext: KeyboardContext? = null

        fun get(): KeyboardContext {
            if (currentContext == null) {
                currentContext = KeyboardContext(
                    keyboardInput = KeyboardInput()
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create keyboard context")
        }
    }

    override fun cleanup() {
    }
}