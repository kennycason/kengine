package com.kengine.sound.synth

import com.kengine.graphics.Color
import com.kengine.hooks.effect.useEffect
import com.kengine.hooks.state.useState
import com.kengine.log.Logging
import com.kengine.ui.FlexDirection
import com.kengine.ui.View
import com.kengine.ui.useView

/**
 * A UI wrapper for controlling an Osc3x synth with sliders, knobs, and waveform buttons.
 */
class Osc3xSynth(
    private val x: Double = 0.0,
    private val y: Double = 0.0,
    defaultVolume: Double = 0.5
) : Logging {

    val width: Double = 530.0
    val height: Double = 90.0

    private val osc3x = Osc3x()

    private val synthView: View

    private val masterVolume = useState(defaultVolume)
    private val volumes = Array(3) { useState(defaultVolume) }
    private val frequencies = Array(3) { useState(440.0) }  // Default to A4
    private val detunes = Array(3) { useState(0.0) }
    private val waveforms = Array(3) { useState(Oscillator.Waveform.SINE) }

    private var lastMasterFrequency = frequencies.map { it.get() }.average()

    init {
        useEffect({ println("Master Volume changed - useEffect") }, masterVolume)

        // Initialize oscillators with default configs
        osc3x.setMasterVolume(masterVolume.get())
        for (i in 0..2) syncOscillatorConfig(i)

        // Build UI
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
            // Master panel
            buildMasterPanel()

            // Oscillator panels
            for (i in 0..2) buildOscillatorPanel(i)
        }
    }

    /**
     * Synchronize the oscillator config with the Osc3x engine.
     */
    private fun syncOscillatorConfig(index: Int) {
        osc3x.setConfig(
            oscillator = index,
            enabled = volumes[index].get() > 0.0,  // Enable if volume > 0
            frequency = frequencies[index].get(),
            waveform = waveforms[index].get(),
            detune = detunes[index].get(),
            volume = volumes[index].get()
        )
    }

    /**
     * Update oscillator configuration and sync state.
     */
    fun updateConfig(
        oscillator: Int,
        frequency: Double? = null,
        detune: Double? = null,
        waveform: Oscillator.Waveform? = null,
        volume: Double? = null
    ) {
        if (oscillator !in 0..2) return // Ignore invalid oscillator index

        // Update state values
        frequency?.let {
            frequencies[oscillator].set(it) // Update frequency state
        }
        detune?.let {
            detunes[oscillator].set(it) // Update detune state
        }
        waveform?.let {
            waveforms[oscillator].set(it) // Update waveform state
        }
        volume?.let {
            volumes[oscillator].set(it) // Update volume state
        }

        // Sync with Osc3x engine
        syncOscillatorConfig(oscillator)
    }

    /**
     * Master volume and controls.
     */
    private fun View.buildMasterPanel() {
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
                onValueChanged = { value ->
                    logger.info("Master volume => $value")
                    osc3x.setMasterVolume(value)
                }
            )
            view(
                id = "master-knob-panel",
                w = 34.0,
                h = 70.0,
                spacing = 2.0,
                direction = FlexDirection.COLUMN
            ) {
                knob(
                    id = "master-osc-frequency-knob",
                    w = 34.0,
                    h = 34.0,
                    padding = 5.0,
                    min = 20.0,
                    max = 20000.0,
                    dragScale = 2000.0,
                    state = useState(frequencies.map { it.get() }.average()),
                    bgColor = Color.neonPeach,
                    onValueChanged = { value ->
                        logger.info("Master frequency knob for osc => $value")

                        val delta = value - lastMasterFrequency
                        for (i in 0..2) {
                            frequencies[i].set((frequencies[i].get() + delta).coerceIn(20.0, 20000.0))
                            syncOscillatorConfig(i)
                        }
                        lastMasterFrequency = value
                    }
                )
                knob(
                    id = "master-osc-detune-knob",
                    w = 34.0,
                    h = 34.0,
                    padding = 5.0,
                    min = -100.0,
                    max = 100.0,
                    state =  useState(detunes.map { it.get() }.average()),
                    bgColor = Color.neonPeach,
                    onValueChanged = { value ->
                        logger.info("Master detune knob for osc => $value")
                        for (i in 0..2) {
                            detunes[i].set(value)
                            syncOscillatorConfig(i)
                        }
                    }
                )
            }

        }
    }

    /**
     * Build panel for an individual oscillator.
     */
    private fun View.buildOscillatorPanel(i: Int) {
        view(
            id = "osc-$i-panel",
            w = 140.0,
            h = 80.0,
            bgColor = Color.neonGreen,
            padding = 5.0,
            spacing = 5.0,
            direction = FlexDirection.ROW
        ) {
            slider(
                id = "osc-${i}-volume-slider",
                w = 20.0,
                h = 70.0,
                min = 0.0,
                max = 1.0,
                bgColor = Color.neonPurple,
                state = volumes[i],
                onValueChanged = { value ->
                    logger.info("Osc $i volume => $value")
                    syncOscillatorConfig(i)
                }
            )
            slider(
                id = "osc-${i}-frequency-slider",
                w = 20.0,
                h = 70.0,
                min = 20.0,
                max = 20000.0,
                bgColor = Color.neonPurple,
                state = frequencies[i],
                onValueChanged = { value ->
                    logger.info("Osc $i frequency => $value")
                    syncOscillatorConfig(i)
                }
            )
            slider(
                id = "osc-${i}-detune-slider",
                w = 20.0,
                h = 70.0,
                min = -100.0,
                max = 100.0,
                bgColor = Color.neonPurple,
                state = detunes[i],
                onValueChanged = { value ->
                    logger.info("Osc $i detune => $value")
                    syncOscillatorConfig(i)
                }
            )

            view(
                id = "knob-${i}-panel",
                w = 34.0,
                h = 70.0,
                spacing = 2.0,
                direction = FlexDirection.COLUMN
            ) {
                // frequency knob
                knob(
                    id = "osc-${i}-frequency-knob",
                    w = 34.0,
                    h = 34.0,
                    padding = 5.0,
                    min = 20.0,
                    max = 20000.0,
                    dragScale = 1000.0,
                    state = frequencies[i],
                    bgColor = Color.neonPeach,
                    onValueChanged = { value ->
                        logger.info("Frequency knob for osc $i => $value")
                        syncOscillatorConfig(i)
                    }
                )

                // detune knob
                knob(
                    id = "osc-${i}-detune-knob",
                    w = 34.0,
                    h = 34.0,
                    padding = 5.0,
                    min = -100.0,
                    max = 100.0,
                    dragScale = 400.0,
                    state = detunes[i],
                    bgColor = Color.neonPeach,
                    onValueChanged = { value ->
                        logger.info("Detune knob for osc $i => $value")
                        syncOscillatorConfig(i)
                    }
                )
            }

            // waveform buttons
            view(
                id = "waveform-$i-column",
                w = 16.0,
                h = 70.0,
                bgColor = Color.neonLime,
                spacing = 2.0,
                direction = FlexDirection.COLUMN
            ) {
                Oscillator.Waveform.entries.forEach { waveform ->
                    button(
                        id = "waveform-button-$i-$waveform",
                        w = 16.0,
                        h = 16.0,
                        bgColor = Color.neonPurple,
                        hoverColor = Color.neonBlue,
                        onClick = {
                            logger.info("Osc $i waveform => $waveform")
                            waveforms[i].set(waveform)
                            syncOscillatorConfig(i)
                        }
                    )
                }
            }
        }
    }

    /**
     * Update audio state (called each frame).
     */
    fun update() {
        osc3x.update()
    }

    /**
     * Render the UI.
     */
    fun draw() {
        synthView.draw()
    }

    /**
     * Access the raw Osc3x engine.
     */
    fun getOsc3x(): Osc3x = osc3x

    fun randomize() {
        osc3x.randomize() // Randomize the underlying Osc3x engine

        // Synchronize the UI state with the randomized values
        for (i in 0..2) {
            val config = osc3x.getConfig(i)
            volumes[i].set(config.volume)
            frequencies[i].set(config.frequency)
            detunes[i].set(config.detune)
            waveforms[i].set(config.waveform)
        }
    }
}
