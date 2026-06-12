package com.kengine.scene

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.action.Easing
import com.kengine.action.getActionContext
import com.kengine.createGameContext
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.hooks.context.ContextRegistry
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.getClockContext
import com.kengine.useGameContext
import kotlin.math.sin
import kotlin.test.Test

class SceneManagementIT {

    @Test
    fun `visual scene management demo`() {
        createGameContext(
            title = "Scene Management Demo",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO
        ) {
            val sceneContext = SceneContext.get()
            ContextRegistry.register(sceneContext)

            GameRunner(frameRate = 60) {
                sceneContext.push(MenuScene(sceneContext))

                object : Game {
                    override fun update() = sceneContext.update()
                    override fun draw() {
                        sceneContext.draw()
                        useSDLContext { flipScreen() }
                    }
                    override fun cleanup() = sceneContext.cleanup()
                }
            }
        }
    }

    private class MenuScene(val scenes: SceneContext) : Scene {
        private var pulseSize = 60.0
        private var orbY = 200.0
        private var elapsed = 0.0

        override fun enter() {
            animatePulse(60.0, 140.0)

            getActionContext().tween(
                from = 200.0, to = 400.0,
                durationMs = 1500,
                easing = Easing.easeInOutBack,
                onUpdate = { orbY = it }
            )

            getActionContext().timer(3000) {
                scenes.push(GameScene(scenes), FadeTransition(600))
            }
        }

        private fun animatePulse(from: Double, to: Double) {
            getActionContext().tween(
                from = from, to = to,
                durationMs = 800,
                easing = Easing.easeInOutSine,
                onUpdate = { pulseSize = it },
                onComplete = { animatePulse(to, from) }
            )
        }

        override fun update() {
            elapsed += getClockContext().deltaTimeSec
        }

        override fun draw() {
            useSDLContext {
                fillScreen(Color(0x10u, 0x10u, 0x30u, 0xFFu))
            }
            useGeometryContext {
                for (i in 0 until 5) {
                    val x = 150.0 + i * 130.0
                    val wobble = sin(elapsed * 2.0 + i * 0.8) * 20.0
                    fillRectangle(x - 20.0, 450.0 + wobble, 40.0, 40.0, Color.teal)
                }

                fillRectangle(
                    400.0 - pulseSize / 2, orbY - pulseSize / 2,
                    pulseSize, pulseSize,
                    Color.cyan
                )
                drawCircle(400.0, orbY, (pulseSize * 0.7).toInt(), Color.white)
                drawCircle(400.0, orbY, (pulseSize * 0.4).toInt(), Color.yellow)
            }
        }
    }

    private class GameScene(val scenes: SceneContext) : Scene {
        private var ballX = -30.0
        private var ballY = 100.0
        private var trailX = DoubleArray(8) { -30.0 }
        private var trailY = DoubleArray(8) { 100.0 }
        private var elapsed = 0.0

        override fun enter() {
            getActionContext().tween(
                from = -30.0, to = 700.0,
                durationMs = 2500,
                easing = Easing.easeOutBounce,
                onUpdate = { ballX = it }
            )
            getActionContext().tween(
                from = 100.0, to = 450.0,
                durationMs = 2500,
                easing = Easing.easeOutElastic,
                onUpdate = { ballY = it }
            )

            getActionContext().timer(3500) {
                scenes.push(PauseScene(scenes), FadeTransition(500))
            }
        }

        override fun update() {
            elapsed += getClockContext().deltaTimeSec
            for (i in trailX.size - 1 downTo 1) {
                trailX[i] = trailX[i - 1]
                trailY[i] = trailY[i - 1]
            }
            trailX[0] = ballX
            trailY[0] = ballY
        }

        override fun draw() {
            useSDLContext {
                fillScreen(Color(0x30u, 0x08u, 0x08u, 0xFFu))
            }
            useGeometryContext {
                for (row in 0 until 12) {
                    for (col in 0 until 16) {
                        val shade = ((row + col) % 2) * 15
                        fillRectangle(
                            col * 50.0, row * 50.0, 50.0, 50.0,
                            Color((0x30 + shade).toUByte(), 0x08u, 0x08u, 0xFFu)
                        )
                    }
                }

                for (i in trailX.size - 1 downTo 0) {
                    val alpha = ((i + 1).toDouble() / trailX.size * 100).toInt().coerceIn(10, 100)
                    val size = 12 + (trailX.size - i) * 2
                    fillRectangle(
                        trailX[i] - size / 2, trailY[i] - size / 2,
                        size.toDouble(), size.toDouble(),
                        Color(0xFFu, 0xD7u, 0x00u, alpha.toUByte())
                    )
                }

                fillRectangle(ballX - 15, ballY - 15, 30.0, 30.0, Color.yellow)
                drawCircle(ballX, ballY, 18, Color.orange)
            }
        }
    }

    private class PauseScene(val scenes: SceneContext) : Scene {
        private var boxScale = 0.0
        private var barAlpha = 0.0

        override fun enter() {
            getActionContext().tween(
                from = 0.0, to = 1.0,
                durationMs = 400,
                easing = Easing.easeOutBack,
                onUpdate = { boxScale = it }
            )
            getActionContext().tween(
                from = 0.0, to = 255.0,
                durationMs = 600,
                easing = Easing.easeOutQuad,
                onUpdate = { barAlpha = it }
            )

            getActionContext().timer(2500) {
                scenes.pop(FadeTransition(500))
            }

            getActionContext().timer(5000) {
                useGameContext { isRunning = false }
            }
        }

        override fun update() {}

        override fun draw() {
            useSDLContext {
                fillScreen(Color(0x05u, 0x05u, 0x15u, 0xFFu))
            }
            useGeometryContext {
                val w = 300.0 * boxScale
                val h = 200.0 * boxScale
                val x = 400.0 - w / 2
                val y = 300.0 - h / 2

                fillRectangle(x, y, w, h, Color(0x30u, 0x30u, 0x50u, 0xFFu))
                drawRectangle(x, y, w, h, Color.silver)

                val a = barAlpha.toInt().coerceIn(0, 255).toUByte()
                val barW = 25.0 * boxScale
                val barH = 60.0 * boxScale
                fillRectangle(370.0 - barW / 2, 300.0 - barH / 2, barW, barH, Color(0xFFu, 0xFFu, 0xFFu, a))
                fillRectangle(430.0 - barW / 2, 300.0 - barH / 2, barW, barH, Color(0xFFu, 0xFFu, 0xFFu, a))
            }
        }
    }
}
