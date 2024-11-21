package com.kengine

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl2.SDL_Delay
import sdl2.SDL_Event
import sdl2.SDL_GetTicks
import sdl2.SDL_PollEvent
import sdl2.SDL_QUIT

class GameLoop(
    val frameRate: Int,
    private val gameLoop: (delta: Double) -> Unit
) {
    private var running = true

    init {
        val frameDelay = 1000.0 / frameRate

        memScoped {
            val event = alloc<SDL_Event>()
            while (running) {
                val frameStart = SDL_GetTicks()

                // poll SDL events TODO should I centralize event polling?
                while (SDL_PollEvent(event.ptr) != 0) {
                    if (event.type == SDL_QUIT) {
                        running = false
                    }
                }

                val frameDelayRatio = frameDelay / 1000.0
                gameLoop(frameDelayRatio) // TODO update to actual time between last frame (some will be shorter/longer than frameDelay)

                // cap frame rate
                val frameTime = (SDL_GetTicks() - frameStart).toDouble()
                if (frameDelay > frameTime) {
                    SDL_Delay((frameDelay - frameTime).toUInt())
                }
            }
        }
    }

    companion object {
        operator fun invoke(frameRate: Int, gameLoop: () -> Unit) {
            GameLoop(frameRate, gameLoop)
        }
    }
}