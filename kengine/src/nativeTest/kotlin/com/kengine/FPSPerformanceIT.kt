package com.kengine

import com.kengine.log.Logger
import com.kengine.sdl.getSDLContext
import com.kengine.time.getClockContext
import com.kengine.time.useTimer
import kotlin.test.Test

class FPSPerformanceIT {
    @Test
    fun `fps test`() {
        val width = 640
        val height = 480
        createGameContext(
            title = "FPS Test",
            width = width,
            height = height,
            logLevel = Logger.Level.INFO
        ) {
            GameRunner(frameRate = -1) {
                val sdlContext = getSDLContext()


                useTimer(10_000L) {
                    isRunning = false
                }

                object : Game {
                    override fun update() {
                        logger.info { "FPS ${getClockContext().fps}ms" }
                    }

                    override fun draw() {
                        sdlContext.fillScreen(0x0u, 0x0u, 0x0u)
                        sdlContext.flipScreen()
                    }

                    override fun cleanup() {}
                }
            }
        }
    }
}
