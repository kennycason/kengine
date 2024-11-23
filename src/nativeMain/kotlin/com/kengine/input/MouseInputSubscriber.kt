package com.kengine.input

import EventContext
import com.kengine.Vec2D
import sdl2.SDL_BUTTON_LEFT
import sdl2.SDL_BUTTON_MIDDLE
import sdl2.SDL_BUTTON_RIGHT
import sdl2.SDL_Event
import sdl2.SDL_GetTicks
import sdl2.SDL_MOUSEBUTTONDOWN
import sdl2.SDL_MOUSEBUTTONUP
import sdl2.SDL_MOUSEMOTION

class MouseInputSubscriber {
    private val buttonStates = mutableMapOf<Int, ButtonState>()
    private var mouseCursor: Vec2D = Vec2D()

    data class ButtonState(
        var isPressed: Boolean = false,
        var lastPressedTime: UInt = 0u
    )

    init {
        EventContext.get()
            .subscribe(EventContext.EventType.MOUSE, ::handleMouseEvent)
    }

    private fun handleMouseEvent(event: SDL_Event) {
        when (event.type) {
            SDL_MOUSEBUTTONDOWN -> {
                val button = event.button.button.toInt()
                if (!buttonStates.containsKey(button)) {
                    buttonStates[button] = ButtonState()
                }
                buttonStates[button]?.apply {
                    isPressed = true
                    lastPressedTime = SDL_GetTicks()
                }
            }
            SDL_MOUSEBUTTONUP -> {
                val button = event.button.button.toInt()
                buttonStates[button]?.isPressed = false
            }
            SDL_MOUSEMOTION -> {
                mouseCursor.x = event.motion.x.toDouble()
                mouseCursor.y = event.motion.y.toDouble()
            }
        }
    }

    /**
     * Get mouse cursor (x,y) position
     */
    fun getCursor(): Vec2D = mouseCursor

    /**
     * Check if a specific mouse button is currently pressed.
     */
    fun isButtonPressed(buttonCode: Int): Boolean {
        return buttonStates[buttonCode]?.isPressed ?: false
    }

    /**
     * Check how much time has passed since a button was last pressed.
     */
    fun timeSinceButtonPressed(buttonCode: Int): UInt {
        val currentTime = SDL_GetTicks()
        return buttonStates[buttonCode]?.let {
            if (it.isPressed) 0u else currentTime - it.lastPressedTime
        } ?: UInt.MAX_VALUE
    }

    // convenience functions for common mouse buttons
    fun isLeftPressed() = isButtonPressed(SDL_BUTTON_LEFT)
    fun isRightPressed() = isButtonPressed(SDL_BUTTON_RIGHT)
    fun isMiddlePressed() = isButtonPressed(SDL_BUTTON_MIDDLE)

    fun timeSinceLeftPressed() = timeSinceButtonPressed(SDL_BUTTON_LEFT)
    fun timeSinceRightPressed() = timeSinceButtonPressed(SDL_BUTTON_RIGHT)
    fun timeSinceMiddlePressed() = timeSinceButtonPressed(SDL_BUTTON_MIDDLE)
}