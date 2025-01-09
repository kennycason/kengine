package com.kengine

import com.kengine.entity.SpriteEntity
import com.kengine.file.File
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Sprite
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.getCurrentMilliseconds
import com.kengine.time.useTimer
import kotlin.test.Test

class BasicGameIT {

    @Test
    fun `basic game test`() {
        createGameContext(
            title = "Render Sprite Test",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO
        ) {
            GameRunner(frameRate = 60) {
                val pokeballSpritePath = File.pwd() + "/src/nativeTest/resources/assets/sprites/pokeball.bmp"
                logger.info(pokeballSpritePath)
                val pokeball = SpriteEntity(Sprite.fromFilePath(pokeballSpritePath))
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

                            useGeometryContext {
                                fillRectangle(10.0, 10.0, 100.0, 100.0, 0x88u, 0x33u, 0xFFu)
                            }
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
