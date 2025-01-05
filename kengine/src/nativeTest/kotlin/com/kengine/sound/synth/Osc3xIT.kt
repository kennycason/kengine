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
import com.kengine.ui.FlexDirection
import com.kengine.ui.useView
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import kotlin.test.Test

class Osc3xIT : Logging {

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun run() {
        createGameContext(
            title = "Kengine - Synth - Osc3x",
            width = 640,
            height = 480,
            logLevel = Logger.Level.TRACE
        ) {

            val osc3x = Osc3x()
            osc3x.setConfig(0, waveform = Oscillator.Waveform.SAW)
            osc3x.setConfig(1, waveform = Oscillator.Waveform.SQUARE)
            osc3x.setConfig(2, waveform = Oscillator.Waveform.SINE)
            osc3x.setMasterVolume(0.3)

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

            val volumeSliderState = useState(0.5)
            val frequencySliderState = useState(444.0)
            val detuneSliderState = useState(0.0)
            val toggleState = useState(false)

            val osc1View = useView(
                id = "sliders",
                x = 0.0,
                y = 0.0,
                w = 80.0,
                h = 80.0,
                bgColor = Color.neonBlue,
                padding = 5.0,
                spacing = 5.0
            ) {
                slider(
                    id = "volume-slider",
                    w = 20.0,
                    h = 70.0,
                    min = 0.0,
                    max = 1.0,
                    padding = 5.0,
                    state = volumeSliderState,
                    bgColor = Color.neonPurple,
                    trackWidth = 3.0,
                    trackColor = Color.neonCyan,
                    handleWidth = 14.0,
                    handleHeight = 7.0,
                    handleColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("slider moved to $value")
                        osc3x.setMasterVolume(value)
                    }
                )
                slider(
                    id = "frequency-slider",
                    w = 20.0,
                    h = 70.0,
                    min = -4000.0,
                    max = 4000.0,
                    padding = 5.0,
                    state = frequencySliderState,
                    bgColor = Color.neonPurple,
                    trackWidth = 3.0,
                    trackColor = Color.neonCyan,
                    handleWidth = 14.0,
                    handleHeight = 7.0,
                    handleColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("slider moved to $value")
                        osc3x.setConfig(frequency = value)
                    }
                )
                slider(
                    id = "detune-slider",
                    w = 20.0,
                    h = 70.0,
                    min = -4000.0,
                    max = 4000.0,
                    padding = 5.0,
                    state = detuneSliderState,
                    bgColor = Color.neonPurple,
                    trackWidth = 3.0,
                    trackColor = Color.neonCyan,
                    handleWidth = 14.0,
                    handleHeight = 7.0,
                    handleColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("slider moved to $value")
                        osc3x.setConfig(detune = value)
                    }
                )
                view(
                    direction = FlexDirection.COLUMN,
                    w = 26.0,
                    h = 64.0 + 6.0,
                    spacing = 2.0
                ) {
                    button(
                        id = "waveform-button1",
                        w = 16.0,
                        h = 16.0,
                        bgColor = Color.neonPurple,
                        hoverColor = Color.neonBlue,
                        pressColor = Color.neonOrange,
                        isCircle = true,
                        onClick = {
                            logger.info("Waveform saw button clicked")
                            osc3x.setConfig(waveform = Oscillator.Waveform.SAW)
                        }
                    )
                    button(
                        id = "waveform-button2",
                        w = 16.0,
                        h = 16.0,
                        bgColor = Color.neonPurple,
                        hoverColor = Color.neonBlue,
                        pressColor = Color.neonOrange,
                        isCircle = true,
                        onClick = {
                            logger.info("Waveform sine button clicked")
                            osc3x.setConfig(waveform = Oscillator.Waveform.SINE)
                        }
                    )
                    button(
                        id = "waveform-button3",
                        w = 16.0,
                        h = 16.0,
                        bgColor = Color.neonPurple,
                        hoverColor = Color.neonBlue,
                        pressColor = Color.neonOrange,
                        isCircle = true,
                        onClick = {
                            logger.info("Waveform triangle button clicked")
                            osc3x.setConfig(waveform = Oscillator.Waveform.TRIANGLE)
                        }
                    )
                    button(
                        id = "waveform-button4",
                        w = 16.0,
                        h = 16.0,
                        bgColor = Color.neonPurple,
                        hoverColor = Color.neonBlue,
                        pressColor = Color.neonOrange,
                        isCircle = true,
                        onClick = {
                            logger.info("Waveform square button clicked")
                            osc3x.setConfig(waveform = Oscillator.Waveform.SQUARE)
                        }
                    )
                }
//                toggleButton(
//                    id = "toggle-button",
//                    w = 48.0,
//                    h = 48.0,
//                    state = toggleState,
//                    padding = 5.0,
//                    bgColor = Color.neonPurple,
//                    hoverColor = Color.neonBlue,
//                    isCircle = true,
//                    onToggle = { value ->
//                        logger.info("Toggle button: $value")
//                        osc3x.setConfig(waveform = Oscillator.Waveform.SINE)
//                    }
//                )
            }

            GameRunner(frameRate = 60) {
                object : Game {

                    override fun update() {
                        useControllerContext {
                            osc1Selected = controller.isButtonPressed(Buttons.L1) && !controller.isButtonPressed(Buttons.R1)
                            osc2Selected = controller.isButtonPressed(Buttons.R1) && !controller.isButtonPressed(Buttons.L1)
                            osc3Selected = controller.isButtonPressed(Buttons.L1) && controller.isButtonPressed(Buttons.R1)

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

//                            logger.infoStream {
//                                write("o1: $osc1Selected o2: $osc2Selected, o3: $osc3Selected,")
//                                write("freq: $deltaFrequency, detune: $deltaDetune, vol=$deltaVolume, wav=$newWaveform")
//                            }

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

//                        rainbowEffect.update()
                        wavePattern.update()
                        wavePattern2.update()
                        SDL_Delay(1u) // small delay to prevent CPU overuse
                    }

                    override fun draw() {
                        useSDLContext {
                            fillScreen(Color.black)
//                            rainbowEffect.draw()
                            wavePattern.draw()
//                            wavePattern2.draw()

                            osc1View.draw()
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
