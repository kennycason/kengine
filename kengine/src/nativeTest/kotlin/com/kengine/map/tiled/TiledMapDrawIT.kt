package com.kengine.map.tiled

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.sdl.useSDLContext
import kotlin.test.Test

class TiledMapDrawIT {

    @Test
    fun `draw ninja turdle map`() {
        val tiledMap = TiledMapLoader()
            .loadMap("src/nativeTest/resources/ninja_turdle_all_tiles.tmj")
        createGameContext(
            title = "Tile Map Test",
            width = 800,
            height = 600
        ) {
            GameRunner(frameRate = 60) {
                object : Game {
                    override fun update() { }

                    override fun draw() {
                        useKeyboardContext {
                            if (keyboard.isUpPressed()) {
                                tiledMap.p.y -= 5
                            }
                            if (keyboard.isDownPressed()) {
                                tiledMap.p.y += 5
                            }
                            if (keyboard.isLeftPressed()) {
                                tiledMap.p.x -= 5
                            }
                            if (keyboard.isRightPressed()) {
                                tiledMap.p.x += 5
                            }
                        }
                        useSDLContext {
                            fillScreen(0u, 0u, 0u)
//                            tiledMap.draw("parallax")
//                            tiledMap.draw("bg")
//                            tiledMap.draw("main")
//                            tiledMap.draw("fg")
                            tiledMap.draw()
                            flipScreen()
                        }
                    }

                    override fun cleanup() { }
                }
            }
        }
    }
}