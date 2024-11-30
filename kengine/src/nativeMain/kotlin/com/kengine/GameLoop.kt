package com.kengine

import com.kengine.context.useContext
import com.kengine.log.Logging
import com.kengine.time.ClockContext
import com.kengine.time.getCurrentTimestampMilliseconds
import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_Delay

@OptIn(ExperimentalForeignApi::class)
class GameLoop(
    frameRate: Int,
    update: () -> Unit
) : Logging {

    init {
        val targetFrameTime = 1000.0 / frameRate
        var lastFrameTimeMs = getCurrentTimestampMilliseconds()

        useContext<GameContext>(cleanup = true) {
            while (isRunning) {
                useContext<ClockContext> {
                    totalTimeMs = getCurrentTimestampMilliseconds()
                    deltaTimeMs = totalTimeMs - lastFrameTimeMs
                    totalTimeSec = totalTimeMs / 1000.0
                    deltaTimeSec = deltaTimeMs / 1000.0
                    lastFrameTimeMs = totalTimeMs
                }

                sdlEvent.pollEvents()
                action.update()

                update()

                val frameTimeMs = getCurrentTimestampMilliseconds() - clock.totalTimeMs
                if (frameTimeMs < targetFrameTime) {
                    SDL_Delay((targetFrameTime - frameTimeMs).toUInt())
                }
            }
        }
        logger.info { "Game loop exited cleanly." }
    }

    companion object {
        operator fun invoke(frameRate: Int, update: (delta: Double) -> Unit): GameLoop {
            return GameLoop(frameRate, update)
        }
    }
}