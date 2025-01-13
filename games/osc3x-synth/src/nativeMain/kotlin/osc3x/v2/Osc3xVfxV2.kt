package osc3x.v2

import com.kengine.graphics.Color
import com.kengine.hooks.state.useState
import com.kengine.log.Logging
import com.kengine.particle.FrequencyCircleEffect
import com.kengine.particle.NeonLinesEffect
import com.kengine.particle.RainbowLinesEffect
import com.kengine.particle.RainbowLinesWithFrequencyEffect
import com.kengine.particle.SacredGeometryOscillation
import com.kengine.particle.SacredGeometryOscillation2
import com.kengine.particle.SpectrographVisualizer
import com.kengine.particle.WavePatternEffect
import com.kengine.particle.WavePatternEffect2
import com.kengine.particle.WaveformGalaxy
import com.kengine.sound.synth.Oscillator
import com.kengine.ui.FlexDirection
import com.kengine.ui.useView
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import kotlin.math.abs

/**
 * A UI wrapper for controlling an Osc3x synth with sliders, knobs, and waveform buttons.
 *
 * osc3xSynth.updateConfig(
 *     oscillator = 0,
 *     frequency = 110.0,
 *     detune = 0.0,
 *     waveform = Oscillator.Waveform.SAW,
 *     lfoEnabled = true,
 *     lfoFrequency = 5.0,
 *     lfoAmplitude = 0.3,
 *     filterEnabled = true,
 *     filterCutoff = 2000.0
 * )
 */
@OptIn(ExperimentalForeignApi::class)
class Osc3xVfxV2(
    private val x: Int = 0,
    private val y: Int = 0,
    private val osc3xSynth: Osc3xSynthV2
) : Logging {

    private val width: Int = 640
    private val height: Int = 480

    val effects = listOf(
        WavePatternEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        RainbowLinesEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
            numLines = width,
        ),
        NeonLinesEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        RainbowLinesWithFrequencyEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
            numLines = width, frequency = 444.0
        ),
        SacredGeometryOscillation2(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        NeonLinesEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        SacredGeometryOscillation(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        WaveformGalaxy(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        SpectrographVisualizer(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
        FrequencyCircleEffect(
            x = 0, y = osc3xSynth.height.toInt(),
            width = width, height = height - osc3xSynth.height.toInt(),
        ),
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
                   // currentEffect.set((currentEffect.get() - 1).mod(effects.size))
                    osc3xSynth.randomize()
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

       // osc3xSynth.update()

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
        } else if (effect is SpectrographVisualizer) {
            effect.setFrequency(
                (if (osc1.volume > 0.0) osc1.frequency else 0.0) +
                    (if (osc2.volume > 0.0) osc2.frequency else 0.0) +
                    (if (osc3.volume > 0.0) osc3.frequency else 0.0)
            )
            effect.setDetune(
                (if (osc1.volume > 0.0) osc1.detune else 0.0) +
                    (if (osc2.volume > 0.0) osc2.detune else 0.0) +
                    (if (osc3.volume > 0.0) osc3.detune else 0.0)
            )
        } else if (effect is WaveformGalaxy) {
            val combinedFrequency = (if (osc1.volume > 0.0) osc1.frequency else 0.0) +
                (if (osc2.volume > 0.0) osc2.frequency else 0.0) +
                (if (osc3.volume > 0.0) osc3.frequency else 0.0)

            val normalizedIndex = ((combinedFrequency / 2000.0) * effect.numStars).toInt().coerceIn(0, effect.numStars - 1)

            // Adjust amplitude for all stars based on frequency
            effect.stars.forEachIndexed { index, _ ->
                val distanceFromIndex = abs(index - normalizedIndex)
                val influence = (1.0 - (distanceFromIndex / effect.numStars.toDouble())).coerceAtLeast(0.0)
                val frequencyEffect = combinedFrequency / 500.0 // Scale by frequency
                effect.setAmplitude(index, influence * frequencyEffect)
            }

            // Apply detune effect
            effect.setDetune(
                (if (osc1.volume > 0.0) osc1.detune else 0.0) +
                    (if (osc2.volume > 0.0) osc2.detune else 0.0) +
                    (if (osc3.volume > 0.0) osc3.detune else 0.0)
            )
        } else if (effect is SacredGeometryOscillation) {
            effect.setFrequency(
                (if (osc1.volume > 0.0) osc1.frequency else 0.0) +
                    (if (osc2.volume > 0.0) osc2.frequency else 0.0) +
                    (if (osc3.volume > 0.0) osc3.frequency else 0.0)
            )
            effect.setDetune(
                (if (osc1.volume > 0.0) osc1.detune else 0.0) +
                    (if (osc2.volume > 0.0) osc2.detune else 0.0) +
                    (if (osc3.volume > 0.0) osc3.detune else 0.0)
            )
        } else if (effect is SacredGeometryOscillation2) {
            effect.setFrequency(
                (if (osc1.volume > 0.0) osc1.frequency else 0.0) +
                    (if (osc2.volume > 0.0) osc2.frequency else 0.0) +
                    (if (osc3.volume > 0.0) osc3.frequency else 0.0)
            )
            effect.setDetune(
                (if (osc1.volume > 0.0) osc1.detune else 0.0) +
                    (if (osc2.volume > 0.0) osc2.detune else 0.0) +
                    (if (osc3.volume > 0.0) osc3.detune else 0.0)
            )
        }
        effect.update()

        SDL_Delay(1u) // small delay to prevent CPU overuse
    }

    fun draw() {
        effects[currentEffect.get()].draw()
        visualizationControls.draw()
    }

    fun cleanup() {
    }
}
