package com.kengine

import sdl2.SDL_Delay
import sdl2.SDL_GetTicks

class GameLoop(
    val frameRate: Int,
    update: (delta: Double) -> Unit
) {
    private var running = true

    init {
        val frameDelay = 1000.0 / frameRate
        while (running) {
            val frameStart = SDL_GetTicks()
            val frameDelayRatio = frameDelay / 1000.0

            update(frameDelayRatio) // TODO update to actual time between last frame (some will be shorter/longer than frameDelay)

            // cap frame rate
            val frameTime = (SDL_GetTicks() - frameStart).toDouble()
            if (frameDelay > frameTime) {
                SDL_Delay((frameDelay - frameTime).toUInt())
            }
        }
    }

    companion object {
        operator fun invoke(frameRate: Int, gameLoop: () -> Unit) {
            GameLoop(frameRate, gameLoop)
        }
    }
}