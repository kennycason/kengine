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
 * pre-resolved cells + UIntArray + no clip inset 1.7ms
 */
class TiledMapDrawIT {

    @Test
    fun `draw ninja turdle map`() {
        runMapDrawTest("src/nativeTest/resources/ninjaturdle/lungs_25.tmj")
    }

    @Test
    fun `draw ninja turdle TMX map`() {
        runMapDrawTest("src/nativeTest/resources/ninjaturdle/lungs_25.tmx")
    }

    private fun runMapDrawTest(mapPath: String) {
        createGameContext(
            title = "Tile Map Test",
            width = 800,
            height = 600,
            logLevel = Logger.Level.DEBUG
        ) {
            GameRunner(frameRate = -1) {
                val tiledMap = TiledMapLoader().loadMap(mapPath)

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
                            tiledMap.draw()

                            val deltaTimeNs = getCurrentNanoseconds() - startTimeNs
                            totalRenderTimesNs += deltaTimeNs
                            iterations++
                            minRenderTimeNs = minOf(minRenderTimeNs, deltaTimeNs)
                            maxRenderTimeNs = maxOf(maxRenderTimeNs, deltaTimeNs)
                            avgRenderTimeNs = (totalRenderTimesNs / iterations.toFloat()).toLong()
                            logger.info {
                                "[$mapPath] rendered in ${deltaTimeNs / 1000000.0}ms, " +
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
