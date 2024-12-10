package com.kengine

import com.kengine.context.useContext
import com.kengine.log.Logging
import com.kengine.time.getClockContext
import com.kengine.time.getCurrentMilliseconds
import kotlinx.cinterop.ExperimentalForeignApi
import sdl2.SDL_Delay

@OptIn(ExperimentalForeignApi::class)
class GameLoop(
    frameRate: Int,
    update: () -> Unit
) : Logging {

    init {
        if (frameRate < 0) {
            logger.info { "Looping as fast as possible." }
        } else {
            logger.info { "Running with $frameRate FPS." }
        }

        val targetFrameTime = 1000.0 / frameRate

        useContext<GameContext>(cleanup = true) {
            while (isRunning) {
                getClockContext().update()

                sdlEvent.pollEvents()
                action.update()

                update()

                if (frameRate > 0) { // set frameRate = -1 to avoid sleep run as fast as possible
                    val frameTimeMs = getCurrentMilliseconds() - clock.totalTimeMs
                    if (frameTimeMs < targetFrameTime) {
                        SDL_Delay((targetFrameTime - frameTimeMs).toUInt())
                    }
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