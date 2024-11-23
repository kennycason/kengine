package com.kengine.input

import EventContext
import sdl2.SDLK_DOWN
import sdl2.SDLK_LEFT
import sdl2.SDLK_RIGHT
import sdl2.SDLK_UP
import sdl2.SDLK_a
import sdl2.SDLK_b
import sdl2.SDL_Event
import sdl2.SDL_GetTicks
import sdl2.SDL_KEYDOWN
import sdl2.SDL_KEYUP
import sdl2.SDL_KeyCode


class KeyboardInputSubscriber {
    private val keyStates = mutableMapOf<Int, KeyState>()

    data class KeyState(
        var isPressed: Boolean = false,
        var lastPressedTime: UInt = 0u
    )

    init {
        EventContext.get()
            .subscribe(EventContext.EventType.KEYBOARD, ::handleKeyboardEvent)
    }

    private fun handleKeyboardEvent(event: SDL_Event) {
        when (event.type) {
            SDL_KEYDOWN -> {
                val key = event.key.keysym.sym
                if (!keyStates.containsKey(key)) {
                    keyStates[key] = KeyState()
                }
                keyStates[key]?.apply {
                    isPressed = true
                    lastPressedTime = SDL_GetTicks()
                }
            }
            SDL_KEYUP -> {
                val key = event.key.keysym.sym
                keyStates[key]?.isPressed = false
            }
        }
    }

    /**
     * check if a specific key is currently pressed
     */
    fun isKeyPressed(keyCode: SDL_KeyCode): Boolean {
        return keyStates[keyCode.toInt()]?.isPressed ?: false
    }

    /**
     * check how much time has passed since a key was last pressed
     */
    fun timeSinceKeyPressed(keyCode: SDL_KeyCode): UInt {
        val currentTime = SDL_GetTicks()
        return keyStates[keyCode.toInt()]?.let {
            if (it.isPressed) 0u else currentTime - it.lastPressedTime
        } ?: UInt.MAX_VALUE // TODO confirm if I want to return this
    }

    fun isUpPressed() = isKeyPressed(SDLK_UP)
    fun isDownPressed() = isKeyPressed(SDLK_DOWN)
    fun isLeftPressed() = isKeyPressed(SDLK_LEFT)
    fun isRightPressed() = isKeyPressed(SDLK_RIGHT)
    fun isAPressed() = isKeyPressed(SDLK_a)
    fun isBPressed() = isKeyPressed(SDLK_b)

    fun timeSinceUpPressed() = timeSinceKeyPressed(SDLK_UP)
    fun timeSinceDownPressed() = timeSinceKeyPressed(SDLK_DOWN)
    fun timeSinceLeftPressed() = timeSinceKeyPressed(SDLK_LEFT)
    fun timeSinceRightPressed() = timeSinceKeyPressed(SDLK_RIGHT)
    fun timeSinceAPressed() = timeSinceKeyPressed(SDLK_a)
    fun timeSinceBPressed() = timeSinceKeyPressed(SDLK_b)
}