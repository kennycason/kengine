package com.kengine.map.tiled

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.getClockContext
import com.kengine.time.getCurrentNanoseconds
import com.kengine.time.useTimer
import kotlin.test.Test

/**
 * Performance:
 * 200ms - v1
 * 100ms - cull edges 100ms
 * minimize object creation + shared textures 15ms
 * optimize tile id lookup 10ms
 * remove/minimize logging 7.5ms
 * Sprite optimize render if no transformation 6.57ms
 * drawNoBatch optimization 6.0ms
 */
class TiledMapDrawIT {

    @Test
    fun `draw ninja turdle map`() {
        createGameContext(
            title = "Tile Map Test",
            width = 800,
            height = 600,
            logLevel = Logger.Level.DEBUG
        ) {
            GameRunner(frameRate = -1) { // unlimited, currently running 130 fps, (map 7ms/render)
                val tiledMap = TiledMapLoader()
//            .loadMap("src/nativeTest/resources/ninjaturdle/stomach_0.tmj")
                    .loadMap("src/nativeTest/resources/ninjaturdle/lungs_25.tmj")
//            .loadMap("src/nativeTest/resources/ninjatuardle/all_tiles.tmj")
//            .loadMap("src/nativeTest/resources/ninjaturdle/single_layer.tmj")
//            .loadMap("src/nativeTest/resources/rotations.tmj")

                object : Game {
                    private val scrollSpeed = 100.0
                    private var totalRenderTimesNs = 0L
                    private var iterations = 0

                    private var minRenderTimeNs = Long.MAX_VALUE
                    private var maxRenderTimeNs = Long.MIN_VALUE
                    private var avgRenderTimeNs = 0L

                    init {
                        useTimer(5000L) {
                            isRunning = false
                        }
                    }

                    override fun update() {
                        tiledMap.update()
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
                            fillScreen(Color.black)
                            val startTimeNs = getCurrentNanoseconds()
//                            tiledMap.draw("bg")
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
                            logger.info { "FPS ${getClockContext().fps}ms" }
                            flipScreen()
                        }
                    }

                    override fun cleanup() {}
                }
            }
        }
    }
}
