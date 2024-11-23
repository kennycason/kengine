package com.kengine.sdl

import EventContext
import com.kengine.log.Logger
import platform.posix.exit
import sdl2.SDL_Event
import sdl2.SDL_Quit

class SDLQuitEventSubscriber {

    init {
        EventContext.get()
            .subscribe(EventContext.EventType.QUIT, ::handleSDLQuitEvent)
    }

    private fun handleSDLQuitEvent(sdlEvent: SDL_Event) {
        Logger.info { "Exiting game" }
        SDL_Quit()
        exit(0)
    }

}
