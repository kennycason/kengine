package com.kengine.map.tiled

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.getCurrentMilliseconds
import com.kengine.time.useTimer
import kotlin.test.Test

class TiledMapDrawIT {

    @Test
    fun `basic game test`() {
        createGameContext(
            title = "Basic Game Test",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO
        ) {
            GameRunner(frameRate = 60) {
                object : Game {
                    private val scrollSpeed = 100.0
                    override fun update() {
                        useTimer(5000L) {
                            isRunning = false
                        }
                    }

                    override fun draw() {
                        val lastMapRenderTimeMs = getCurrentMilliseconds()
                        useSDLContext {
                            fillScreen(0u, 0u, 0u)
                            flipScreen()
                        }
                        logger.info { "Game loop in ${getCurrentMilliseconds() - lastMapRenderTimeMs}ms" }
                    }

                    override fun cleanup() {}
                }
            }
        }
    }
}
