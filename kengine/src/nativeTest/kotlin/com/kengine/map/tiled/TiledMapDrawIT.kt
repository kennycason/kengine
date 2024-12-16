package com.kengine.map.tiled

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.getClockContext
import com.kengine.time.useTimer
import kotlin.test.Test

class TiledMapDrawIT {

    @Test
    fun `draw ninja turdle map`() {
        val tiledMap = TiledMapLoader()
//            .loadMap("src/nativeTest/resources/ninjaturdle/stomach_0.tmj")
            .loadMap("src/nativeTest/resources/ninjaturdle/lungs_25.tmj")
//            .loadMap("src/nativeTest/resources/ninjaturdle/all_tiles.tmj")
//            .loadMap("src/nativeTest/resources/ninjaturdle/single_layer.tmj")
//            .loadMap("src/nativeTest/resources/rotations.tmj")
        createGameContext(
            title = "Tile Map Test",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO
        ) {
            GameRunner(frameRate = 60) {
                object : Game {
                    private val scrollSpeed = 100.0
                    override fun update() {
                        useTimer(60000L) { // end after 60 seconds
                            isRunning = false
                        }
                    }

                    override fun draw() {
                        val deltaTime = getClockContext().deltaTimeSec
                        useKeyboardContext {
                            if (keyboard.isUpPressed()) {
                                tiledMap.p.y -= scrollSpeed * deltaTime
                            }
                            if (keyboard.isDownPressed()) {
                                tiledMap.p.y += scrollSpeed * deltaTime
                            }
                            if (keyboard.isLeftPressed()) {
                                tiledMap.p.x -= scrollSpeed * deltaTime
                            }
                            if (keyboard.isRightPressed()) {
                                tiledMap.p.x += scrollSpeed * deltaTime
                            }
                        }
                        useSDLContext {
                            fillScreen(0u, 0u, 0u)
                            tiledMap.draw()
                            flipScreen()
                        }
                    }

                    override fun cleanup() {}
                }
            }
        }
    }
}