package com.kengine

import com.kengine.context.useContext
import com.kengine.log.Logger
import sdl2.SDL_Delay
import sdl2.SDL_GetTicks

class GameLoop(
    frameRate: Int,
    update: (elapsedSeconds: Double) -> Unit
) {

    init {
        val targetFrameTime = 1000.0 / frameRate
        var lastFrameTime = SDL_GetTicks().toDouble()
        val gameContext = GameContext.get()

        while (gameContext.isRunning) {
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
        Logger.info { "Game loop exited cleanly."}
        GameContext.get().cleanup()
    }

    companion object {
        operator fun invoke(frameRate: Int, update: (delta: Double) -> Unit): GameLoop {
            return GameLoop(frameRate, update)
        }
    }
}