package com.kengine.sound.synth

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.log.Logger
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import kotlin.test.Test

class Osc3xSynthDemo : Logging {

    @Test
    fun run() {
        createGameContext(
            title = "Kengine - Osc3x Synth",
            width = 640,
            height = 480,
            logLevel = Logger.Level.INFO
        ) {

            val osc3xSynth = Osc3xSynth(
                x = 0.0, y = 0.0, defaultVolume = 0.05
            )
            val osc3XVfx = Osc3xVfx(
                x = 0, y = osc3xSynth.height.toInt(),
                osc3xSynth = osc3xSynth
            )

            GameRunner(frameRate = 60) {
                object : Game {

                    override fun update() {
                        osc3xSynth.update()
                        osc3XVfx.update()
                    }

                    override fun draw() {
                        useSDLContext {
                            fillScreen(Color.black)
                            osc3xSynth.draw()
                            osc3XVfx.draw()
                            flipScreen()
                        }
                    }

                    override fun cleanup() {
                    }
                }
            }
        }
    }
}
