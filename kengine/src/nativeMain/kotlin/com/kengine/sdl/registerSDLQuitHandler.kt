package com.kengine.sdl

import com.kengine.getGameContext
import com.kengine.log.getLogger
import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_Event

private val logger = getLogger("main")

@OptIn(ExperimentalForeignApi::class)
fun registerSDLQuitHandler() {
    fun handleSDLQuitEvent(sdlEvent: SDL_Event) {
        logger.info { "Exiting game" }
        getGameContext().isRunning = false
    }

    getSDLEventContext()
        .subscribe(SDLEventContext.EventType.QUIT, ::handleSDLQuitEvent)
}
