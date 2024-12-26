package com.kengine.input.mouse

import com.kengine.math.Vec2
import com.kengine.sdl.SDLEventContext
import com.kengine.sdl.useSDLEventContext
import com.kengine.time.getCurrentMilliseconds
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_BUTTON_LEFT
import sdl3.SDL_BUTTON_MIDDLE
import sdl3.SDL_BUTTON_RIGHT
import sdl3.SDL_EVENT_MOUSE_BUTTON_DOWN
import sdl3.SDL_EVENT_MOUSE_BUTTON_UP
import sdl3.SDL_EVENT_MOUSE_MOTION
import sdl3.SDL_Event


@OptIn(ExperimentalForeignApi::class)
class MouseInputEventSubscriber {
    private val buttonStates = mutableMapOf<Int, ButtonState>()
    private var mouseCursor: Vec2 = Vec2()

    data class ButtonState(
        var isPressed: Boolean = false,
        var lastPressedTime: Long = 0
    )

    // must be called
    fun init() {
        useSDLEventContext {
            logger.info { "Subscribed to mouse events" }
            subscribe(SDLEventContext.EventType.MOUSE, ::handleMouseEvent)
        }
    }

    fun handleMouseEvent(event: SDL_Event) {
        when (event.type) {
            SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                val button = event.button.button.toInt()
                if (!buttonStates.containsKey(button)) {
                    buttonStates[button] = ButtonState()
                }
                buttonStates[button]?.apply {
                    isPressed = true
                    lastPressedTime = getCurrentMilliseconds()
                }
            }
            SDL_EVENT_MOUSE_BUTTON_UP -> {
                val button = event.button.button.toInt()
                buttonStates[button]?.isPressed = false
            }
            SDL_EVENT_MOUSE_MOTION -> {
                mouseCursor.x = event.motion.x.toDouble()
                mouseCursor.y = event.motion.y.toDouble()
            }
        }
    }

    /**
     * Get mouse cursor (x,y) position
     */
    fun getCursor(): Vec2 = mouseCursor

    /**
     * Check if a specific mouse button is currently pressed.
     */
    fun isButtonPressed(buttonCode: Int): Boolean {
        return buttonStates[buttonCode]?.isPressed ?: false
    }

    /**
     * Check how much time has passed since a button was last pressed.
     */
    fun timeSinceButtonPressed(buttonCode: Int): Long {
        val currentTime = getCurrentMilliseconds()
        return buttonStates[buttonCode]?.let {
            if (it.isPressed) 0L else currentTime - it.lastPressedTime
        } ?: Long.MAX_VALUE
    }

    fun isLeftPressed() = isButtonPressed(SDL_BUTTON_LEFT)
    fun isRightPressed() = isButtonPressed(SDL_BUTTON_RIGHT)
    fun isMiddlePressed() = isButtonPressed(SDL_BUTTON_MIDDLE)

    fun timeSinceLeftPressed() = timeSinceButtonPressed(SDL_BUTTON_LEFT)
    fun timeSinceRightPressed() = timeSinceButtonPressed(SDL_BUTTON_RIGHT)
    fun timeSinceMiddlePressed() = timeSinceButtonPressed(SDL_BUTTON_MIDDLE)
}
