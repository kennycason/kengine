package com.kengine.sound.synth

import com.kengine.graphics.Color
import com.kengine.hooks.state.useState
import com.kengine.log.Logging
import com.kengine.particle.FrequencyCircleEffect
import com.kengine.particle.RainbowLinesEffect
import com.kengine.particle.RainbowLinesWithFrequencyEffect
import com.kengine.particle.WavePatternEffect
import com.kengine.particle.WavePatternEffect2
import com.kengine.ui.FlexDirection
import com.kengine.ui.useView
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay

/**
 * A UI wrapper for controlling an Osc3x synth with sliders, knobs, and waveform buttons.
 */
@OptIn(ExperimentalForeignApi::class)
class Osc3xVfx(
    private val x: Int = 0,
    private val y: Int = 0,
    private val osc3xSynth: Osc3xSynth
) : Logging {

    private val width: Int = 640
    private val height: Int = 480

    val effects = listOf(
        WavePatternEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        FrequencyCircleEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        RainbowLinesEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
            numLines = width,
        ),
        RainbowLinesWithFrequencyEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
            numLines = width, frequency = 444.0
        )
    )
    private val currentEffect = useState(0)

    private val visualizationControls = useView(
        id = "visualization-controls",
        x = 530.0,
        y = 0.0,
        w = 110.0,
        h = 90.0,
        bgColor = Color.neonBlue,
        padding = 5.0,
        spacing = 5.0,
        direction = FlexDirection.ROW
    ) {
        view(
            direction = FlexDirection.COLUMN,
            w = 35.0,
            h = 80.0,
            spacing = 4.0,
        ) {
            button(
                id = "visualisation-change-button-1",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = Color.neonMagenta,
                hoverColor = Color.neonPurple,
                onClick = {
                    logger.info("Clicked pad 1")
                    currentEffect.set((currentEffect.get() + 1).mod(effects.size))
                }
            )
            button(
                id = "visualisation-change-button-2",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = Color.neonTurquoise,
                hoverColor = Color.neonMagenta,
                onClick = {
                    logger.info("Clicked pad 2")
                    currentEffect.set((currentEffect.get() - 1).mod(effects.size))
                }
            )
        }
        view(
            direction = FlexDirection.COLUMN,
            w = 35.0,
            h = 80.0,
            spacing = 4.0,
        ) {
            button(
                id = "visualisation-change-button-3",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = Color.neonPink,
                hoverColor = Color.neonMagenta,
                onClick = {
                    logger.info("Clicked pad 3")
                    osc3xSynth.updateConfig(0, frequency = 110.0, detune = 0.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.updateConfig(1, frequency = 110.0, detune = 50.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.updateConfig(2, frequency = 110.0, detune = -50.0, waveform = Oscillator.Waveform.SAW)
                }
            )
            button(
                id = "visualisation-change-button-4",
                w = 35.0,
                h = 38.0,
                padding = 5.0,
                bgColor = Color.neonOrange,
                hoverColor = Color.neonMagenta,
                onClick = {
                    logger.info("Clicked pad 4")
                    osc3xSynth.updateConfig(0, frequency = 98.0, detune = 30.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.updateConfig(1, frequency = 98.0, detune = -30.0, waveform = Oscillator.Waveform.SAW)
                    osc3xSynth.updateConfig(2, frequency = 196.0, detune = 0.0, waveform = Oscillator.Waveform.SQUARE)
                }
            )
        }
        slider(
            id = "visualization-slider",
            w = 20.0,
            h = 80.0,
            min = 0.0,
            max = 1.0,
            state = useState(0.0),
            bgColor = Color.neonPurple,
            trackWidth = 3.0,
            trackColor = Color.neonCyan,
            handleWidth = 14.0,
            handleHeight = 7.0,
            handleColor = Color.neonOrange,
            onValueChanged = { value ->
                logger.info("Visualization slider => $value")
            }
        )
    }

    fun update() {
        val config = osc3xSynth.getOsc3x()
        val osc1 = config.getConfig(0)
        val osc2 = config.getConfig(1)
        val osc3 = config.getConfig(2)

        osc3xSynth.update()

        val effect = effects[currentEffect.get()]
        if (effect is WavePatternEffect) {
            effect.setFrequency(
                (if (osc1.volume > 0.0) osc1.frequency else 0.0) +
                    (if (osc2.volume > 0.0) osc2.frequency else 0.0) +
                    (if (osc3.volume > 0.0) osc3.frequency else 0.0)
            )
        } else if (effect is WavePatternEffect2) {
            effect.setFrequency(
                (if (osc1.volume > 0.0) osc1.frequency else 0.0) +
                    (if (osc2.volume > 0.0) osc2.frequency else 0.0) +
                    (if (osc3.volume > 0.0) osc3.frequency else 0.0)
            )
        } else if (effect is FrequencyCircleEffect) {
            effect.setFrequency(
                (if (osc1.volume > 0.0) osc1.frequency else 0.0) +
                    (if (osc2.volume > 0.0) osc2.frequency else 0.0) +
                    (if (osc3.volume > 0.0) osc3.frequency else 0.0)
            )
        } else if (effect is RainbowLinesWithFrequencyEffect) {
            effect.setFrequency(
                (if (osc1.volume > 0.0) osc1.frequency else 0.0) +
                    (if (osc2.volume > 0.0) osc2.frequency else 0.0) +
                    (if (osc3.volume > 0.0) osc3.frequency else 0.0)
            )
        }
        effect.update()

        SDL_Delay(1u) // small delay to prevent CPU overuse
    }

    fun draw() {
        visualizationControls.draw()
        effects[currentEffect.get()].draw()
    }

    fun cleanup() {
    }
}
