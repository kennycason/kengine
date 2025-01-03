package com.kengine.sound.synth

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.hooks.effect.useEffect
import com.kengine.hooks.state.useState
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.useControllerContext
import com.kengine.log.Logger
import com.kengine.log.Logging
import com.kengine.particle.RainbowLinesEffect
import com.kengine.particle.WavePatternEffect
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import kotlin.test.Test

class SynthKeyboardIT : Logging {

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun run() {
        createGameContext(
            title = "Kengine - Synth",
            width = 640,
            height = 480,
            logLevel = Logger.Level.DEBUG
        ) {

            val frequency = useState(440.0)
            val audioStream = AudioStream(frequency = frequency.get())

            val rainbowEffect = RainbowLinesEffect(
                x = 0, y = 0,
                width = 640, height = 480, numLines = 640
            )
            val wavePatternEffect = WavePatternEffect(
                x = 0, y = 0,
                width = 640, height = 480,
                numWaves = 128
            )

            useEffect({
                logger.info("frequency update effect: ${frequency.get()}")
                audioStream.setFrequency(frequency.get())
                // nap frequency to color offset in the rainbow
                val offset = ((frequency.get() - 100) / 10).toInt() % 256
                rainbowEffect.setOffset(offset)
            }, frequency)

            useEffect({
                logger.info("frequency update effect: ${frequency.get()}")
                audioStream.setFrequency(frequency.get())
                wavePatternEffect.setFrequency(frequency.get()) // Update wave pattern
            }, frequency)


            GameRunner(frameRate = 60) {
                object : Game {

                    override fun update() {
                        useControllerContext {
                            if (controller.isButtonPressed(Buttons.L1)) {
                                frequency.set(frequency.get() - 5.0)
                            }
                            if (controller.isButtonPressed(Buttons.R1)) {
                                frequency.set(frequency.get() + 5.0)
                            }
                            if (controller.isButtonPressed(Buttons.A)) {
                                audioStream.pause()
                            }
                            if (controller.isButtonPressed(Buttons.B)) {
                                audioStream.resume()
                            }
                        }

                        audioStream.update()
                        rainbowEffect.update()
                        wavePatternEffect.update()
                        SDL_Delay(1u) // Small delay to prevent CPU overuse
                    }

                    override fun draw() {
                        useSDLContext {
                            fillScreen(Color.black)
//                            rainbowEffect.draw()
                            wavePatternEffect.draw()
                            flipScreen()
                        }
                    }

                    override fun cleanup() {
                        // audioStream.cleanup()
                    }
                }
            }
        }
    }
}
