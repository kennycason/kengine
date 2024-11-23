package com.kengine.sdl

import com.kengine.log.Logger
import platform.posix.exit
import sdl2.SDL_Event
import sdl2.SDL_Quit

fun useSDLQuitEventSubscriber() {
    fun handleSDLQuitEvent(sdlEvent: SDL_Event) {
        Logger.info { "Exiting game" }
        SDL_Quit()
        exit(0)
    }

    SDLEventContext.get()
        .subscribe(SDLEventContext.EventType.QUIT, ::handleSDLQuitEvent)
}
