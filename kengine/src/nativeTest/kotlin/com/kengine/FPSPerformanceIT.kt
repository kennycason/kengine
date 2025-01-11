package com.kengine

import com.kengine.log.Logger
import com.kengine.time.getClockContext
import com.kengine.time.useTimer
import kotlin.test.Test

class GravitySimulationIT {

    @Test
    fun `fps test`() {
        val width = 800
        val height = 600
        createGameContext(
            title = "FPS Test",
            width = width,
            height = height,
            logLevel = Logger.Level.INFO
        ) {
            GameRunner(frameRate = 60) {
                object : Game {
                    override fun update() {
                        useTimer(30000L) {
                            isRunning = false
                        }
                    }

                    override fun draw() {
                        logger.info { "FPS ${getClockContext().fps}ms" }
                    }

                    override fun cleanup() {}
                }
            }
        }
    }
}
