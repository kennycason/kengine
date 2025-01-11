package com.kengine.particle

import com.kengine.entity.Actor
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class WaveformGalaxy(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    val numStars: Int = 500
) : Actor {
    val stars = MutableList(numStars) {
        Star(
            angle = Random.nextDouble(0.0, 2 * PI), // Random starting angle
            radius = Random.nextDouble(0.0, width / 2.5), // Ensure radius fits comfortably
            speed = Random.nextDouble(0.01, 0.06), // Random speed
            amplitude = 0.5, // Default amplitude
            color = Color.rainbow(numStars)[it % numStars] // Cycle through rainbow colors
        )
    }
    private val centerX = x + width / 2.0
    private val centerY = y + height / 2.0
    private var detune = 0.0
    private var time = 0.0

    fun setAmplitude(index: Int, amplitude: Double) {
        stars[index % stars.size].amplitude = amplitude.coerceIn(0.0, 1.0)
    }

    fun setDetune(newDetune: Double) {
        detune = newDetune.coerceIn(-100.0, 100.0)
    }

    override fun update() {
        time += 0.02 // Adjust time for smooth rotation

        stars.forEach { star ->
            // Update the star's angle based on its speed and detune
            star.angle += star.speed + (detune * 0.0001)

            // Calculate the star's position based on its amplitude and radius
            val adjustedRadius = star.amplitude * star.radius
            star.x = centerX + adjustedRadius * cos(star.angle)
            star.y = centerY + adjustedRadius * sin(star.angle)
        }
    }

    override fun draw() {
        useGeometryContext {
            stars.forEach { star ->
                val starSize = 1 + (star.amplitude * 4).toInt() // Star size based on amplitude
                fillCircle(
                    star.x,
                    star.y,
                    starSize,
                    star.color.r,
                    star.color.g,
                    star.color.b,
                    0xFFu
                )
            }
        }
    }

    data class Star(
        var angle: Double, // Current angle of the star
        var radius: Double, // Distance from the center
        var speed: Double, // Speed of rotation
        var amplitude: Double, // Amplitude multiplier for the star
        var x: Double = 0.0, // Current x-coordinate
        var y: Double = 0.0, // Current y-coordinate
        val color: Color // Color of the star
    )
}
