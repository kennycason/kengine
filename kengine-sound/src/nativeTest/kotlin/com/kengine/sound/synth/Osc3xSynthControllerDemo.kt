package com.kengine.sound.synth

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.hooks.state.useState
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.useControllerContext
import com.kengine.log.Logger
import com.kengine.log.Logging
import com.kengine.particle.RainbowLinesEffect
import com.kengine.particle.WavePatternEffect
import com.kengine.particle.WavePatternEffect2
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import kotlin.test.Ignore
import kotlin.test.Test

class Osc3xSynthControllerDemo : Logging {

    @OptIn(ExperimentalForeignApi::class)
    @Test
    @Ignore // @Test to demo
    fun run() {
        createGameContext(
            title = "Kengine - Synth - Osc3x - Controller Demo",
            width = 640,
            height = 480,
            logLevel = Logger.Level.INFO
        ) {

            var osc1Selected = false
            var osc2Selected = false
            var osc3Selected = false

            val wavePattern = WavePatternEffect(
                x = 0, y = 0,
                width = 640, height = 480,
            )
            val wavePattern2 = WavePatternEffect2(
                x = 0, y = 0,
                width = 640, height = 480,
            )

            val rainbowEffect = RainbowLinesEffect(
                x = 0, y = 0,
                width = 640, height = 480, numLines = 640
            )

            val volumeState = useState(0.05)
            val frequencyState = useState(444.0)
            val detuneState = useState(0.0)

            val osc3x = Osc3x()
            osc3x.setConfig(0, waveform = Oscillator.Waveform.SAW)
            osc3x.setConfig(1, waveform = Oscillator.Waveform.SQUARE)
            osc3x.setConfig(2, waveform = Oscillator.Waveform.SINE)
            osc3x.setMasterVolume(volumeState.get())
            osc3x.setConfig(frequency = frequencyState.get(), detune = detuneState.get())

            GameRunner(frameRate = 60) {
                object : Game {

                    override fun update() {
                        useControllerContext {
                            osc1Selected =
                                controller.isButtonPressed(Buttons.L1) && !controller.isButtonPressed(Buttons.R1)
                            osc2Selected =
                                controller.isButtonPressed(Buttons.R1) && !controller.isButtonPressed(Buttons.L1)
                            osc3Selected =
                                controller.isButtonPressed(Buttons.L1) && controller.isButtonPressed(Buttons.R1)

                            val deltaDetune = when {
                                controller.isButtonPressed(Buttons.DPAD_UP) -> 1.0
                                controller.isButtonPressed(Buttons.DPAD_DOWN) -> -1.0
                                else -> 0.0
                            }

                            val deltaFrequency = when {
                                controller.isButtonPressed(Buttons.DPAD_LEFT) -> -5.0
                                controller.isButtonPressed(Buttons.DPAD_RIGHT) -> 5.0
                                else -> 0.0
                            }

                            val newWaveform = when {
                                controller.isButtonPressed(Buttons.X) -> Oscillator.Waveform.SINE
                                controller.isButtonPressed(Buttons.Y) -> Oscillator.Waveform.SAW
                                controller.isButtonPressed(Buttons.B) -> Oscillator.Waveform.SQUARE
                                controller.isButtonPressed(Buttons.A) -> Oscillator.Waveform.TRIANGLE
                                else -> null // no change if no waveform button is pressed
                            }

                            val deltaVolume = when {
                                controller.isButtonPressed(Buttons.L3) -> -0.01 // Decrease volume
                                controller.isButtonPressed(Buttons.R3) -> 0.01  // Increase volume
                                else -> 0.0
                            }

                            if (logger.isDebugEnabled()) {
                                logger.debugStream {
                                    write("o1: $osc1Selected o2: $osc2Selected, o3: $osc3Selected,")
                                    write("freq: $deltaFrequency, detune: $deltaDetune, vol=$deltaVolume, wav=$newWaveform")
                                }
                            }

                            // apply changes for the selected oscillator(s)
                            fun applyUpdates(index: Int) {
                                if (deltaDetune != 0.0 || deltaFrequency != 0.0) {
                                    val config = osc3x.getConfig(index)
                                    osc3x.setConfig(
                                        index,
                                        detune = config.detune + deltaDetune,
                                        frequency = config.frequency + deltaFrequency
                                    )

                                    if (osc3x.countEnabled() > 0) {
                                        wavePattern.setFrequency(
                                            osc3x.getConfig(0).frequency +
                                                    osc3x.getConfig(1).frequency +
                                                    osc3x.getConfig(2).frequency
                                        )
                                        wavePattern2.setFrequency(
                                            osc3x.getConfig(0).frequency +
                                                    osc3x.getConfig(1).frequency +
                                                    osc3x.getConfig(2).frequency
                                        )
                                    }
                                }
                                newWaveform?.let { osc3x.setConfig(index, waveform = it) } // update waveform

                                if (deltaVolume != 0.0) {
                                    val currentConfig = osc3x.getConfig(index)
                                    val newVolume = (currentConfig.volume + deltaVolume).coerceIn(0.0, 1.0)
                                    osc3x.setConfig(index, volume = newVolume)
                                }
                            }

                            if (osc1Selected) {
                                applyUpdates(0)
                            }
                            if (osc2Selected) {
                                applyUpdates(1)
                            }
                            if (osc3Selected) {
                                applyUpdates(2)
                            }
                            if (!(osc1Selected || osc2Selected || osc3Selected)) {
                                applyUpdates(0)
                                applyUpdates(1)
                                applyUpdates(2)
                            }
                        }

                        osc3x.update()
                        wavePattern.update()
                        SDL_Delay(1u) // small delay to prevent CPU overuse
                    }

                    override fun draw() {
                        useSDLContext {
                            fillScreen(Color.Companion.black)
                            wavePattern.draw()

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
