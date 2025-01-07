package com.kengine.sound.synth

import com.kengine.graphics.Color
import com.kengine.hooks.effect.useEffect
import com.kengine.hooks.state.useState
import com.kengine.log.Logging
import com.kengine.ui.FlexDirection
import com.kengine.ui.View
import com.kengine.ui.useView

/**
 * A simple wrapper that creates a UI for controlling an Osc3x.
 *
 * TODO make skin-able
 */
class Osc3xSynth(
    private val x: Double = 0.0,
    private val y: Double = 0.0,
    defaultVolume: Double = 0.5
    // Optionally top-level container size, etc.:
) : Logging {
    val width: Double = 530.0
    val height: Double = 90.0
    private val osc3x = Osc3x()

    // Master volume and states for each oscillator’s frequency, detune, etc.
    private val masterVolume = useState(defaultVolume)
    private val masterFrequency = useState(444.00)
    private val masterDetune = useState(0.00)

    // individual oscillator settings
    private val volumes = Array(3) { useState(defaultVolume) }
    private val frequencies = Array(3) { useState(444.0) }
    private val detunes = Array(3) { useState(0.0) }

    private val synthView: View

    init {
        // 1) Initialize the Osc3x engine defaults
        osc3x.setConfig(0, waveform = Oscillator.Waveform.SAW)
        osc3x.setConfig(1, waveform = Oscillator.Waveform.SQUARE)
        osc3x.setConfig(2, waveform = Oscillator.Waveform.SINE)

        // Set master volume & freq/detune from our states
        osc3x.setMasterVolume(masterVolume.get())
        for (i in 0..2) {
            osc3x.setConfig(
                oscillator = i,
                frequency = frequencies[i].get(),
                detune = detunes[i].get()
            )
        }

        useEffect(
            {
                masterFrequency.set(
                    (frequencies[0].get() + frequencies[1].get() + frequencies[2].get()) / 3
                )
            },
            frequencies[0], frequencies[1], frequencies[2]
        )
        useEffect(
            {
                masterDetune.set(
                    (detunes[0].get() + detunes[1].get() + detunes[2].get()) / 3
                )
            },
            detunes[0], detunes[1], detunes[2]
        )


        synthView = useView(
            id = "osc3xsynth-container",
            x = x,
            y = y,
            w = width,
            h = height,
            direction = FlexDirection.ROW,
            padding = 5.0,
            spacing = 5.0,
            bgColor = Color.neonPink
        ) {
            // A small sub-panel for master volume + master frequency/detune knobs
            view(
                id = "master-panel",
                w = 70.0,
                h = 80.0,
                bgColor = Color.neonBlue,
                padding = 5.0,
                spacing = 5.0,
                direction = FlexDirection.ROW
            ) {
                slider(
                    id = "master-volume-slider",
                    w = 20.0,
                    h = 70.0,
                    min = 0.0,
                    max = 1.0,
                    state = masterVolume,
                    bgColor = Color.neonPurple,
                    trackWidth = 3.0,
                    trackColor = Color.neonCyan,
                    handleWidth = 14.0,
                    handleHeight = 7.0,
                    handleColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("Master volume slider => $value")
                        osc3x.setMasterVolume(value)
                    }
                )
                view(
                    direction = FlexDirection.COLUMN,
                    w = 35.0,
                    h = 70.0,
                    bgColor = Color.neonMagenta
                ) {
                    knob(
                        id = "osc-master-frequency-knob",
                        w = 35.0,
                        h = 35.0,
                        padding = 5.0,
                        min = -4000.0,
                        max = 4000.0,
                        state = masterFrequency,
                        bgColor = Color.neonPeach,
                        knobColor = Color.neonCyan,
                        indicatorColor = Color.neonOrange,
                        onValueChanged = { value ->
                            logger.info("Master frequency knob for osc => $value")
                            osc3x.setConfig(frequency = value)
                        }
                    )
                    knob(
                        id = "osc-master-detune-knob",
                        w = 35.0,
                        h = 35.0,
                        padding = 5.0,
                        min = -4000.0,
                        max = 4000.0,
                        state = masterDetune,
                        bgColor = Color.neonPeach,
                        knobColor = Color.neonCyan,
                        indicatorColor = Color.neonOrange,
                        onValueChanged = { value ->
                            logger.info("Master detune knob for osc => $value")
                            osc3x.setConfig(frequency = value)
                        }
                    )
                }
            }

            // build 3 sub-panels for the 3 oscillators
            for (i in 0..2) {
                buildOscillatorPanel(i)
            }

        }
    }

    /**
     * A helper that adds a sub-panel for oscillator[i].
     * Called inside the top-level container's init block.
     */
    private fun View.buildOscillatorPanel(i: Int) {
        view(
            id = "osc-panel-$i",
            w = 145.0,
            h = 80.0,
            bgColor = Color.neonGreen,
            padding = 5.0,
            spacing = 5.0,
            direction = FlexDirection.ROW
        ) {
            // column for frequency & detune sliders
            view(
                id = "sliders-row-$i",
                direction = FlexDirection.ROW,
                w = 70.0,
                h = 70.0,
                spacing = 5.0
            ) {
                // freq slider
                slider(
                    id = "osc${i}-volume-slider",
                    w = 20.0,
                    h = 70.0,
                    min = 0.0,
                    max = 1.0,
                    state = volumes[i],
                    bgColor = Color.neonPurple,
                    trackWidth = 3.0,
                    trackColor = Color.neonCyan,
                    handleWidth = 14.0,
                    handleHeight = 7.0,
                    handleColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("Osc $i freq => $value")
                        osc3x.setConfig(oscillator = i, volume = value)
                    }
                )
                slider(
                    id = "osc${i}-frequency-slider",
                    w = 20.0,
                    h = 70.0,
                    min = -4000.0,
                    max = 4000.0,
                    state = frequencies[i],
                    bgColor = Color.neonPurple,
                    trackWidth = 3.0,
                    trackColor = Color.neonCyan,
                    handleWidth = 14.0,
                    handleHeight = 7.0,
                    handleColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("Osc $i freq => $value")
                        osc3x.setConfig(oscillator = i, frequency = value)
                    }
                )
                // detune slider
                slider(
                    id = "osc${i}-detune-slider",
                    w = 20.0,
                    h = 70.0,
                    min = -4000.0,
                    max = 4000.0,
                    state = detunes[i],
                    bgColor = Color.neonPurple,
                    trackWidth = 3.0,
                    trackColor = Color.neonCyan,
                    handleWidth = 14.0,
                    handleHeight = 7.0,
                    handleColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("Osc $i detune => $value")
                        osc3x.setConfig(oscillator = i, detune = value)
                    }
                )
            }

            // column for waveform selection
            view(
                id = "waveform-col-$i",
                direction = FlexDirection.COLUMN,
                w = 20.0,
                h = 70.0,
                bgColor = Color.neonLime,
                padding = 2.0,
                spacing = 3.0
            ) {
                fun buildWaveformButton(wf: Oscillator.Waveform) {
                    button(
                        id = "waveform-button-$i-$wf",
                        w = 14.0,
                        h = 14.0,
                        bgColor = Color.neonPurple,
                        hoverColor = Color.neonBlue,
                        pressColor = Color.neonOrange,
                        isCircle = true,
                        onClick = {
                            logger.info("Osc $i waveform => $wf")
                            osc3x.setConfig(oscillator = i, waveform = wf)
                        }
                    )
                }

                buildWaveformButton(Oscillator.Waveform.SAW)
                buildWaveformButton(Oscillator.Waveform.SINE)
                buildWaveformButton(Oscillator.Waveform.TRIANGLE)
                buildWaveformButton(Oscillator.Waveform.SQUARE)
            }

            view(
                direction = FlexDirection.COLUMN,
                w = 35.0,
                h = 70.0,
                bgColor = Color.neonMagenta
            ) {
                knob(
                    id = "osc${i}-frequency-knob",
                    w = 35.0,
                    h = 35.0,
                    padding = 5.0,
                    min = -4000.0,
                    max = 4000.0,
                    state = frequencies[i],
                    bgColor = Color.neonPeach,
                    knobColor = Color.neonCyan,
                    indicatorColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("Frequency knob for osc $i => $value")
                        osc3x.setConfig(oscillator = i, frequency = value)
                    }
                )
                knob(
                    id = "osc${i}-detune-knob",
                    w = 35.0,
                    h = 35.0,
                    padding = 5.0,
                    min = -4000.0,
                    max = 4000.0,
                    state = detunes[i],
                    bgColor = Color.neonPeach,
                    knobColor = Color.neonCyan,
                    indicatorColor = Color.neonOrange,
                    onValueChanged = { value ->
                        logger.info("Detune knob for osc $i => $value")
                        osc3x.setConfig(oscillator = i, detune = value)
                    }
                )
            }
        }
    }

    fun update() {
        osc3x.update()
    }

    /**
     * Either draw all views with getViewContext().render() or render this view specifically
     */
    fun draw() {
        synthView.draw()
    }

    /**
     * If you want direct access to the engine from outside:
     */
    fun getOsc3x(): Osc3x = osc3x
}
