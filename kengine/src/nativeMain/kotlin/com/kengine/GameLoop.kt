package com.kengine

import com.kengine.context.useContext
import com.kengine.log.Logger
import com.kengine.time.getCurrentTimestampMilliseconds
import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_Delay

@OptIn(ExperimentalForeignApi::class)
class GameLoop(
    frameRate: Int,
    update: (elapsedSeconds: Double) -> Unit
) {

    init {
        val targetFrameTime = 1000.0 / frameRate
        var lastFrameTime = getCurrentTimestampMilliseconds().toDouble()

        useContext(GameContext.get(), cleanup = true) {
            while (isRunning) {
                val currentFrameTime = getCurrentTimestampMilliseconds().toDouble()
                val elapsedSeconds = (currentFrameTime - lastFrameTime) / 1000.0
                lastFrameTime = currentFrameTime

                events.pollEvents()
                actions.update(elapsedSeconds)

                update(elapsedSeconds)

                val frameTime = getCurrentTimestampMilliseconds().toDouble() - currentFrameTime
                if (frameTime < targetFrameTime) {
                    SDL_Delay((targetFrameTime - frameTime).toUInt())
                }
            }
        }
        Logger.info { "Game loop exited cleanly." }
    }

    companion object {
        operator fun invoke(frameRate: Int, update: (delta: Double) -> Unit): GameLoop {
            return GameLoop(frameRate, update)
        }
    }
}