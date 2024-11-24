package com.kengine

import com.kengine.context.useContext
import sdl2.SDL_Delay
import sdl2.SDL_GetTicks

class GameLoop(
    frameRate: Int,
    update: (elapsedSeconds: Double) -> Unit
) {
    private var running = true

    fun stop() {
        running = false
    }

    init {
        val targetFrameTime = 1000.0 / frameRate
        var lastFrameTime = SDL_GetTicks().toDouble()

        while (running) {
            val currentFrameTime = SDL_GetTicks().toDouble()
            val elapsedSeconds = (currentFrameTime - lastFrameTime) / 1000.0
            lastFrameTime = currentFrameTime

            useContext(GameContext.get()) {
                events.pollEvents()
                actions.update(elapsedSeconds)
            }

            update(elapsedSeconds)

            val frameTime = SDL_GetTicks().toDouble() - currentFrameTime
            if (frameTime < targetFrameTime) {
                SDL_Delay((targetFrameTime - frameTime).toUInt())
            }
        }
    }

    companion object {
        operator fun invoke(frameRate: Int, update: (delta: Double) -> Unit): GameLoop {
            return GameLoop(frameRate, update)
        }
    }
}