package com.kengine.sound.synth

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.hooks.state.State
import com.kengine.hooks.state.useState
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.useControllerContext
import com.kengine.log.Logger
import com.kengine.log.Logging
import com.kengine.particle.RainbowLinesEffect
import com.kengine.particle.WavePatternEffect
import com.kengine.particle.WavePatternEffect2
import com.kengine.sdl.useSDLContext
import com.kengine.ui.Align
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

            val volumeState = useState(0.5)
            val frequencyState = useState(444.0)
            val detuneState = useState(0.0)
            val knobState = useState(0.0)

            val osc1View = useView(
                id = "sliders",
                x = 0.0,
                y = 0.0,
                w = 145.0,
                h = 80.0,
                bgColor = Color.neonBlue,
                padding = 5.0,
                spacing = 5.0
            ) {
                fun buildSlider(
                    id: String,
                    min: Double, max: Double,
                    state: State<Double>,
                    onValueChanged: (newValue: Double) -> Unit
                ) {
                    slider(
                        id = id,
                        w = 20.0,
                        h = 70.0,
                        min = min,
                        max = max,
                        padding = 5.0,
                        state = state,
                        bgColor = Color.neonPurple,
                        trackWidth = 3.0,
                        trackColor = Color.neonCyan,
                        handleWidth = 14.0,
                        handleHeight = 7.0,
                        handleColor = Color.neonOrange,
                        onValueChanged = onValueChanged
                    )
                }
                buildSlider(
                    id = "volume-slider",
                    min = 0.0,
                    max = 1.0,
                    state = volumeState,
                    onValueChanged = { value ->
                        logger.info("slider moved to $value")
                        osc3x.setMasterVolume(value)
                    }
                )
                buildSlider(
                    id = "frequency-slider",
                    min = -4000.0,
                    max = 4000.0,
                    state = frequencyState,
                    onValueChanged = { value ->
                        logger.info("slider moved to $value")
                        osc3x.setConfig(frequency = value)
                    }
                )
                buildSlider(
                    id = "detune-slider",
                    min = -4000.0,
                    max = 4000.0,
                    state = detuneState,
                    onValueChanged = { value ->
                        logger.info("slider moved to $value")
                        osc3x.setConfig(detune = value)
                    }
                )

                view(
                    direction = FlexDirection.COLUMN,
                    align = Align.LEFT,
                    w = 20.0,
                    h = 70.0,
                    padding = 2.0,
                    bgColor = Color.neonLime,
                    spacing = 3.0
                ) {
                    fun buildWaveFormButton(waveform: Oscillator.Waveform) {
                        button(
                            id = "waveform-button-$waveform",
                            w = 14.0,
                            h = 14.0,
                            bgColor = Color.neonPurple,
                            hoverColor = Color.neonBlue,
                            pressColor = Color.neonOrange,
                            isCircle = true,
                            onClick = {
                                logger.info("Waveform $waveform button clicked")
                                osc3x.setConfig(waveform = waveform)
                            }
                        )
                    }
                    buildWaveFormButton(Oscillator.Waveform.SAW)
                    buildWaveFormButton(Oscillator.Waveform.SINE)
                    buildWaveFormButton(Oscillator.Waveform.TRIANGLE)
                    buildWaveFormButton(Oscillator.Waveform.SQUARE)
                }

                view(
                    direction = FlexDirection.COLUMN,
                    align = Align.LEFT,
                    w = 35.0,
                    h = 70.0,
                    padding = 0.0,
                    bgColor = Color.neonMagenta,
                ) {
                    knob(
                        id = "frequency-knob",
                        w = 35.0,
                        h = 35.0,
                        padding = 5.0,
                        min = -4000.0,
                        max = 4000.0,
                        stepSize = 100.0,
                        state = knobState,
                        bgColor = Color.neonPeach,
                        knobColor = Color.neonCyan,
                        indicatorColor = Color.neonOrange,
                        onValueChanged = { value ->
                            logger.info("Knob moved to $value")
                            osc3x.setConfig(frequency = value)
                        }
                    )
                    knob(
                        id = "volume-knob",
                        w = 35.0,
                        h = 35.0,
                        padding = 5.0,
                        min = 0.0,
                        max = 1.0,
                        stepSize = 0.001,  // Will change by 0.01 per step
                        state = volumeState,
                        bgColor = Color.neonPurple,
                        knobColor = Color.neonCyan,
                        indicatorColor = Color.neonOrange,
                        onValueChanged = { value ->
                            logger.info("Volume knob moved to $value")
                            osc3x.setMasterVolume(value)
                        }
                    )
                }

            }

//            val testView = useView(
//                id = "parent",
//                x = 300.0,
//                y = 100.0,
//                w = 200.0,
//                h = 200.0,
//                padding = 10.0,
//                bgColor = Color.gray40
//            ) {
//                button(
//                    id = "nested1",
//                    x = 20.0,
//                    y = 20.0,
//                    w = 50.0,
//                    h = 50.0,
//                    bgColor = Color.green,
//                    onClick = { println("Nested button 1 clicked") }
//                )
//                button(
//                    id = "nested2",
//                    x = 100.0,
//                    y = 20.0,
//                    w = 50.0,
//                    h = 50.0,
//                    bgColor = Color.yellow,
//                    onClick = { println("Nested button 2 clicked") }
//                )
//            }

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
//                            testView.draw()
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
