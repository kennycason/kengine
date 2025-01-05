package com.kengine.input.mouse

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class MouseState {
    var isLeftPressed = false
    var isRightPressed = false
    var wasLeftPressed = false
    var wasRightPressed = false
}

class MouseContext private constructor(
    val mouse: MouseInputEventSubscriber
) : Context(), Logging {

    private val state = MouseState()

    fun wasLeftReleased(): Boolean {
        val wasPressed = state.wasLeftPressed
        val isPressed = mouse.isLeftPressed()
        state.wasLeftPressed = isPressed
        return wasPressed && !isPressed
    }

    fun wasRightReleased(): Boolean {
        val wasPressed = state.wasRightPressed
        val isPressed = mouse.isRightPressed()
        state.wasRightPressed = isPressed
        return wasPressed && !isPressed
    }

    fun isLeftDragging(): Boolean {
        return state.isLeftPressed && mouse.isLeftPressed()
    }

    fun isRightDragging(): Boolean {
        return state.isRightPressed && mouse.isRightPressed()
    }

    override fun cleanup() {
        logger.info { "Cleaning up MouseContext" }
        currentContext = null
    }

    fun init() {
        mouse.init()
    }

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

}
