package com.kengine.action

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.useTimer
import kotlin.test.Test

class TweenEasingIT {

    @Test
    fun `visual tween easing demo`() {
        createGameContext(
            title = "Tween Easing Demo",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO
        ) {
            GameRunner(frameRate = 60) {
                val easings = listOf(
                    "linear" to Easing.linear,
                    "easeInQuad" to Easing.easeInQuad,
                    "easeOutQuad" to Easing.easeOutQuad,
                    "easeInOutQuad" to Easing.easeInOutQuad,
                    "easeInCubic" to Easing.easeInCubic,
                    "easeOutCubic" to Easing.easeOutCubic,
                    "easeInOutCubic" to Easing.easeInOutCubic,
                    "easeInBack" to Easing.easeInBack,
                    "easeOutBack" to Easing.easeOutBack,
                    "easeOutBounce" to Easing.easeOutBounce,
                    "easeOutElastic" to Easing.easeOutElastic,
                    "easeInOutElastic" to Easing.easeInOutElastic,
                )

                val rowHeight = 600.0 / easings.size
                val startX = 100.0
                val endX = 700.0
                val positions = DoubleArray(easings.size) { startX }
                val colors = listOf(
                    Color.red, Color.green, Color.blue, Color.yellow,
                    Color.cyan, Color.magenta, Color.orange, Color.purple,
                    Color.pink, Color.coral, Color.teal, Color.gold
                )

                object : Game {
                    private var launched = false

                    init {
                        useTimer(8000L) { isRunning = false }
                    }

                    override fun update() {
                        if (!launched) {
                            launched = true
                            val ctx = getActionContext()
                            easings.forEachIndexed { i, (name, easing) ->
                                ctx.tween(
                                    from = startX, to = endX,
                                    durationMs = 3000,
                                    easing = easing,
                                    onUpdate = { v -> positions[i] = v },
                                    onComplete = {
                                        ctx.tween(
                                            from = endX, to = startX,
                                            durationMs = 3000,
                                            easing = easing,
                                            onUpdate = { v -> positions[i] = v }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    override fun draw() {
                        useSDLContext {
                            fillScreen(Color.black)
                            useGeometryContext {
                                easings.forEachIndexed { i, _ ->
                                    val y = i * rowHeight + rowHeight / 2
                                    val color = colors[i % colors.size]
                                    drawLine(startX, y, endX, y, Color.silver)
                                    fillRectangle(
                                        positions[i] - 8, y - 8,
                                        16.0, 16.0,
                                        color
                                    )
                                }
                            }
                            flipScreen()
                        }
                    }

                    override fun cleanup() {}
                }
            }
        }
    }
}
