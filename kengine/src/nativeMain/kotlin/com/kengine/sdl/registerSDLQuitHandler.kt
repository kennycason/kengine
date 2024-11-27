package com.kengine.sdl

import com.kengine.GameContext
import com.kengine.log.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_Event

@OptIn(ExperimentalForeignApi::class)
fun registerSDLQuitHandler() {
    fun handleSDLQuitEvent(sdlEvent: SDL_Event) {
        Logger.info { "Exiting game" }
        GameContext.get().isRunning = false
    }

    SDLEventContext.get()
        .subscribe(SDLEventContext.EventType.QUIT, ::handleSDLQuitEvent)
}
