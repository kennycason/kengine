package com.kengine.context

import EventContext
import com.kengine.graphics.TextureManagerContext
import com.kengine.input.KeyboardContext
import com.kengine.input.MouseContext
import com.kengine.sdl.SDLContext
import com.kengine.sdl.useSDLQuitEventSubscriber

class GameContext private constructor(
    val sdl: SDLContext,
    val events: EventContext,
    val keyboard: KeyboardContext,
    val mouse: MouseContext,
    val textureManager: TextureManagerContext
) : Context() {
    init {
        useSDLQuitEventSubscriber()
    }

    companion object {
        private var currentContext: GameContext? = null

        fun create(
            title: String,
            width: Int,
            height: Int
        ): GameContext {
            if (currentContext != null) {
                throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }
            currentContext = GameContext(
                sdl = SDLContext.create(title, width, height),
                events = EventContext.get(),
                keyboard = KeyboardContext.get(),
                mouse = MouseContext.get(),
                textureManager = TextureManagerContext.get()
            )
            return currentContext!!
        }

        fun get(): GameContext {
            return currentContext ?: throw IllegalStateException("AppContext has not been created. Call create() first.")
        }

    }

    override fun cleanup() {
        sdl.cleanup()
        keyboard.cleanup()
        mouse.cleanup()
        events.cleanup()
        textureManager.cleanup()
    }

}