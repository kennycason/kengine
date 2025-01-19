package osc3x.v2

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
class Osc3xVfx(
    private val x: Int = 0,
    private val y: Int = 0,
    private val osc3xSynth: Osc3xSynth
) : Logging {

    private val width: Int = 640
    private val height: Int = 480

    private val effects = listOf(
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

    fun update() {
        val config = osc3xSynth.osc3x
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

      //  SDL_Delay(1u) // small delay to prevent CPU overuse
    }

    fun draw() {
        effects[currentEffect.get()].draw()
    }

    fun cleanup() {
    }

    fun nextEffect() {
        currentEffect.set((currentEffect.get() + 1).mod(effects.size))
    }

    fun previousEffect() {
         currentEffect.set((currentEffect.get() - 1).mod(effects.size))
    }
}
