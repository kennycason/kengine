package com.kengine.input

import com.kengine.context.useContext
import com.kengine.sdl.SDLContext
import com.kengine.sdl.SDLEventContext
import sdl2.SDLK_DOWN
import sdl2.SDLK_LEFT
import sdl2.SDLK_RIGHT
import sdl2.SDLK_UP
import sdl2.SDLK_a
import sdl2.SDLK_b
import sdl2.SDLK_d
import sdl2.SDLK_s
import sdl2.SDLK_w
import sdl2.SDL_Event
import sdl2.SDL_GetTicks
import sdl2.SDL_KEYDOWN
import sdl2.SDL_KEYUP
import sdl2.SDL_KeyCode


class KeyboardInputEventSubscriber {
    private val keyStates = mutableMapOf<Int, KeyState>()

    data class KeyState(
        var isPressed: Boolean = false,
        var lastPressedTime: UInt = 0u
    )

    init {
        useContext(SDLContext.get()) {
            events.subscribe(SDLEventContext.EventType.KEYBOARD, ::handleKeyboardEvent)
        }
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

    fun isUpPressed() = isKeyPressed(SDLK_UP) || isKeyPressed(SDLK_w)
    fun isDownPressed() = isKeyPressed(SDLK_DOWN)|| isKeyPressed(SDLK_s)
    fun isLeftPressed() = isKeyPressed(SDLK_LEFT)|| isKeyPressed(SDLK_a)
    fun isRightPressed() = isKeyPressed(SDLK_RIGHT)|| isKeyPressed(SDLK_d)
    fun isAPressed() = isKeyPressed(SDLK_a)
    fun isBPressed() = isKeyPressed(SDLK_b)

    fun timeSinceUpPressed() = timeSinceKeyPressed(SDLK_UP)
    fun timeSinceDownPressed() = timeSinceKeyPressed(SDLK_DOWN)
    fun timeSinceLeftPressed() = timeSinceKeyPressed(SDLK_LEFT)
    fun timeSinceRightPressed() = timeSinceKeyPressed(SDLK_RIGHT)
    fun timeSinceAPressed() = timeSinceKeyPressed(SDLK_a)
    fun timeSinceBPressed() = timeSinceKeyPressed(SDLK_b)
}