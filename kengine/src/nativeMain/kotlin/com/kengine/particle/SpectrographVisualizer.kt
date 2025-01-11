package com.kengine.particle

import com.kengine.entity.Actor
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import kotlin.math.max
import kotlin.math.sin

class SpectrographVisualizer(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val numBands: Int = 64,
    private val decayRate: Double = 0.01 // How quickly the bars decay per frame
) : Actor {
    private val bands = MutableList(numBands) { 0.0 }
    private val colors = Color.neon(numBands)
    private var detune = 0.0 // detune value in cents (-100 to +100)
    private var frequency = 440.0 // default frequency
    private var time = 0.0   // time for wave evolution

    fun setBandValue(index: Int, value: Double) {
        bands[index] = max(bands[index], value.coerceIn(0.0, 1.0)) // Normalize and prevent abrupt drops
    }

    fun setFrequency(newFrequency: Double) {
        frequency = newFrequency
    }

    fun setDetune(newDetune: Double) {
        detune = newDetune.coerceIn(-100.0, 100.0)
    }

    override fun draw() {
        useGeometryContext {
            val bandWidth = width / numBands.toDouble()
            for (i in bands.indices) {
                val bandHeight = (bands[i] * height).coerceIn(1.0, height.toDouble())
                val hueShift = (detune / 100.0) * 360.0 // Map detune to hue shift

                // Gradient effect along the height
                for (h in 0 until bandHeight.toInt()) {
                    val intensity = h.toDouble() / bandHeight
                    val color = Color.applyHueShift(
                        Color.linearInterpolate(
                            colors[i],
                            Color.black,
                            1.0 - intensity
                        ), hueShift.toFloat()
                    )

                    // Draw thin horizontal lines for gradient
                    fillRectangle(
                        x + i * bandWidth,
                        y + height - h.toDouble(),
                        bandWidth,
                        1.0,
                        color.r,
                        color.g,
                        color.b,
                        0xFFu
                    )
                }
            }
        }
    }

    override fun update() {
        time += 0.1
        for (i in bands.indices) {
            // Frequency-based oscillation for dynamic behavior
            val oscillation = 0.5 * sin(i * 0.1 + time * (frequency * 0.002))
            bands[i] = max(0.0, bands[i] - decayRate + oscillation)
            bands[i] = bands[i].coerceIn(0.0, 1.0) // Ensure values stay normalized
        }
    }
}
