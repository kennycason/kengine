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
            val osc3x = Osc3x()
            osc3x.setConfig(0, waveform = Oscillator.Waveform.SAW)
            osc3x.setConfig(1, waveform = Oscillator.Waveform.SQUARE)
            osc3x.setConfig(2, waveform = Oscillator.Waveform.SINE)

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
                // nap frequency to color offset in the rainbow
                val offset = ((frequency.get() - 100) / 10).toInt() % 256
                rainbowEffect.setOffset(offset)
            }, frequency)

            useEffect({
                logger.info("frequency update effect: ${frequency.get()}")
                wavePatternEffect.setFrequency(frequency.get()) // Update wave pattern
            }, frequency)


            GameRunner(frameRate = 60) {
                object : Game {

                    override fun update() {
                        useControllerContext {
                            val osc1Selected = controller.isButtonPressed(Buttons.L1)
                            val osc2Selected = controller.isButtonPressed(Buttons.L2)
                            val osc3Selected = controller.isButtonPressed(Buttons.R1)
                            val oscAllSelected = controller.isButtonPressed(Buttons.R2)

                            // Adjust detune or frequency for the selected oscillator(s)
                            val detuneChange = when {
                                controller.isButtonPressed(Buttons.DPAD_UP) -> 1.0
                                controller.isButtonPressed(Buttons.DPAD_DOWN) -> -1.0
                                else -> 0.0
                            }

                            val frequencyChange = when {
                                controller.isButtonPressed(Buttons.DPAD_LEFT) -> -10.0
                                controller.isButtonPressed(Buttons.DPAD_RIGHT) -> 10.0
                                else -> 0.0
                            }

                            val newWaveform = when {
                                controller.isButtonPressed(Buttons.X) -> Oscillator.Waveform.SINE
                                controller.isButtonPressed(Buttons.Y) -> Oscillator.Waveform.SAW
                                controller.isButtonPressed(Buttons.B) -> Oscillator.Waveform.SQUARE
                                controller.isButtonPressed(Buttons.A) -> Oscillator.Waveform.TRIANGLE
                                else -> null // No change if no waveform button is pressed
                            }

                            val volumeChange = when {
                                controller.isButtonPressed(Buttons.L3) -> -0.05 // Decrease volume
                                controller.isButtonPressed(Buttons.R3) -> 0.05  // Increase volume
                                else -> 0.0
                            }

                            // apply changes for the selected oscillator(s)
                            fun applyUpdates(index: Int) {
                                if (detuneChange != 0.0 || frequencyChange != 0.0) {
                                    val config = osc3x.getConfig(index)
                                    osc3x.setConfig(index,
                                        detune = config.detune + detuneChange,
                                        frequency = config.frequency + frequencyChange
                                    )
                                }
                                newWaveform?.let { osc3x.setConfig(index, waveform = it) } // update waveform

                                if (volumeChange != 0.0) {
                                    val currentConfig = osc3x.getConfig(index)
                                    val newVolume = (currentConfig.volume + volumeChange).coerceIn(0.0, 1.0)
                                    osc3x.setConfig(index, volume = newVolume)
                                }
                            }

                            if (osc1Selected) {
                                applyUpdates(0)
                            } else if (osc2Selected) {
                                applyUpdates(1)
                            } else if (osc3Selected) {
                                applyUpdates(2)
                            } else if (oscAllSelected) {
                                applyUpdates(0) // apply to all oscillators
                                applyUpdates(1)
                                applyUpdates(2)
                            }
                        }

                        osc3x.update()
                      //  rainbowEffect.update()
                        wavePatternEffect.update()
                        SDL_Delay(1u) // small delay to prevent CPU overuse
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
