package com.kengine

import com.kengine.log.Logging
import com.kengine.time.getClockContext
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay

@OptIn(ExperimentalForeignApi::class)
class GameLoop(
    frameRate: Int,
    update: () -> Unit
) : Logging {

    init {
        val clock = getClockContext()
        clock.setFrameRate(frameRate) // Let Clock manage FPS logic

        if (frameRate < 0) {
            logger.info { "Running as fast as possible." }
        } else {
            logger.info { "Running at $frameRate FPS." }
        }

        // Main game loop
        useGameContext(cleanup = true) {
            while (isRunning) {
                // Update clock (handles FPS tracking & frame drops)
                clock.update()

                // Process events and actions
                sdlEvent.pollEvents()
                action.update()

                // Execute game-specific update logic
                update()

                // Frame rate limiting handled by Clock
                val delay = clock.calculateFrameDelay()
                if (delay > 0) {
                    SDL_Delay(delay.toUInt())
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
