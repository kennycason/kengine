package com.kengine.map.tiled

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.getClockContext
import com.kengine.time.getCurrentNanoseconds
import com.kengine.time.useTimer
import kotlin.test.Test

class TiledMapDrawIT {

    @Test
    fun `draw ninja turdle map`() {
        createGameContext(
            title = "Tile Map Test",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO
        ) {
            GameRunner(frameRate = 60) {
                val tiledMap = TiledMapLoader()
//            .loadMap("src/nativeTest/resources/ninjaturdle/stomach_0.tmj")
                    .loadMap("src/nativeTest/resources/ninjaturdle/lungs_25.tmj")
//            .loadMap("src/nativeTest/resources/ninjaturdle/all_tiles.tmj")
//            .loadMap("src/nativeTest/resources/ninjaturdle/single_layer.tmj")
//            .loadMap("src/nativeTest/resources/rotations.tmj")

                object : Game {
                    private val scrollSpeed = 100.0
                    private var totalRenderTimesNs = 0L
                    private var iterations = 0

                    private var minRenderTimeNs = Long.MAX_VALUE
                    private var maxRenderTimeNs = Long.MIN_VALUE
                    private var avgRenderTimeNs = 0L

                    override fun update() {
                        useTimer(5000L) {
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
                            val startTimeNs = getCurrentNanoseconds()
//                            tiledMap.p.set(100.0, 100.0)
                            tiledMap.update()
                            tiledMap.draw()

                            val deltaTimeNs = getCurrentNanoseconds() - startTimeNs  // This now measures actual draw time
                            totalRenderTimesNs += deltaTimeNs
                            iterations++
                            minRenderTimeNs = minOf(minRenderTimeNs, deltaTimeNs)
                            maxRenderTimeNs = maxOf(maxRenderTimeNs, deltaTimeNs)
                            avgRenderTimeNs = (totalRenderTimesNs / iterations.toFloat()).toLong()
                            logger.info {
                                "Map rendered in ${deltaTimeNs / 1000000.0}ms, " +
                                    "avg: ${avgRenderTimeNs / 1000000.0}ms " +
                                    "min: ${minRenderTimeNs / 1000000.0}ms " +
                                    "max: ${maxRenderTimeNs / 1000000.0}ms"
                            }
                            flipScreen()
                        }
                    }

                    override fun cleanup() {}
                }
            }
                .also { cleanup() }
        }
    }
}
