package com.kengine.sdl

import com.kengine.GameContext
import com.kengine.log.Logger
import sdl2.SDL_Event

fun useSDLQuitEventSubscriber() {
    fun handleSDLQuitEvent(sdlEvent: SDL_Event) {
        Logger.info { "Exiting game" }
        GameContext.get().isRunning = false
    }

    SDLEventContext.get()
        .subscribe(SDLEventContext.EventType.QUIT, ::handleSDLQuitEvent)
}
