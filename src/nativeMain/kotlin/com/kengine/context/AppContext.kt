package com.kengine.context

import EventContext
import com.kengine.graphics.TextureManager
import com.kengine.input.KeyboardContext
import com.kengine.input.MouseContext
import com.kengine.sdl.SDLContext

class AppContext private constructor(
    val sdl: SDLContext,
    val events: EventContext,
    val keyboard: KeyboardContext,
    val mouse: MouseContext
) : Context() {

    companion object {
        private var currentContext: AppContext? = null

        fun create(
            title: String,
            width: Int,
            height: Int
        ): AppContext {
            if (currentContext != null) {
                throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }
            currentContext = AppContext(
                sdl = SDLContext.create(title, width, height),
                events = EventContext.get(),
                keyboard = KeyboardContext.get(),
                mouse = MouseContext.get()
            )
            return currentContext!!
        }

        fun get(): AppContext {
            return currentContext ?: throw IllegalStateException("AppContext has not been created. Call create() first.")
        }

    }

    override fun cleanup() {
        sdl.cleanup()
        keyboard.cleanup()
        mouse.cleanup()
        events.cleanup()
        TextureManager.cleanup()
    }
}