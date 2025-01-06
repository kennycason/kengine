package com.kengine

import com.kengine.input.mouse.useMouseContext
import com.kengine.log.Logging
import com.kengine.sdl.SDLEventContext
import com.kengine.time.getClockContext
import com.kengine.ui.getViewContext
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import sdl3.SDL_GetPerformanceCounter
import sdl3.SDL_GetPerformanceFrequency

@OptIn(ExperimentalForeignApi::class)
class GameLoop(
    private val frameRate: Int,               // Target FPS (-1 for uncapped)
    private val update: () -> Unit,           // Game update logic
    private val draw: () -> Unit              // Game draw/render logic
) : Logging {

    fun start() {
        val clock = getClockContext()
        val eventContext = SDLEventContext.get()

        // Timing setup
        val frequency = SDL_GetPerformanceFrequency()
        var lastCounter = SDL_GetPerformanceCounter()

        // Target frame duration in milliseconds
        val targetFrameMs = if (frameRate > 0) 1000.0 / frameRate else 0.0
        val targetFrameNs = (targetFrameMs * 1_000_000.0).toLong()

        // Set FPS in ClockContext
        clock.setFrameRate(frameRate)

        logger.info {
            if (frameRate < 0) "Running at uncapped FPS."
            else "Target Frame Rate: $frameRate FPS."
        }

        useGameContext(cleanup = true) {
            while (isRunning) {
                // poll events
                eventContext.pollEvents()
                action.update()

                // calculate delta time
                val currentCounter = SDL_GetPerformanceCounter()
                val deltaTimeNs = (currentCounter - lastCounter) * 1_000_000_000u / frequency
                lastCounter = currentCounter

                // update ClockContext
                clock.update((deltaTimeNs / 1_000_000u).toLong()) // Convert ns -> ms

                useMouseContext {
                    val cursor = mouse.cursor()

                    // Release if left was just released
                    if (wasLeftReleased()) {
                        getViewContext().releaseMouseEvents(cursor.x, cursor.y)
                    }
                    // Press or hover
                    getViewContext().handleMouseEvents(
                        cursor.x,
                        cursor.y,
                        mouse.isLeftPressed()
                    )
                }

                update()

                getViewContext().performLayout()

                draw()

                // apply frame delay if capped
                val elapsedNs = (SDL_GetPerformanceCounter() - currentCounter) * 1_000_000_000u / frequency
                val delayNs = targetFrameNs - elapsedNs.toLong()

                if (delayNs > 0) {
                    SDL_Delay((delayNs / 1_000_000).toUInt()) // Convert ns -> ms
                }
            }
        }

        logger.info { "Game loop exited cleanly." }
    }


    companion object {
        operator fun invoke(
            frameRate: Int,
            update: (delta: Double) -> Unit,
            draw: () -> Unit
        ): GameLoop {
            return GameLoop(frameRate, { update(getClockContext().deltaTimeSec) }, draw)
        }
    }
}
