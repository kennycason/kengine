package com.kengine

import com.kengine.entity.SpriteEntity
import com.kengine.graphics.Sprite
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
                val pokeball = SpriteEntity(Sprite.fromFilePath("src/nativeTest/assets/pokeball.bmp"))
                pokeball.p.set(100.0, 100.0)

                object : Game {
                    override fun update() {
                        useTimer(5000L) {
                            isRunning = false
                        }
                    }

                    override fun draw() {
                        val lastMapRenderTimeMs = getCurrentMilliseconds()
                        useSDLContext {
                            fillScreen(0u, 0u, 0u)
                            pokeball.draw()
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
