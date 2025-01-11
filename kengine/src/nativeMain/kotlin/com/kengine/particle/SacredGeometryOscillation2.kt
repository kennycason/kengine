package com.kengine.particle

import com.kengine.entity.Actor
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.math.Vec2
import com.kengine.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SacredGeometryOscillation2(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val numShapes: Int = 6
) : Actor {
    private val centerX = x + width / 2.0
    private val centerY = y + height / 2.0
    private var time = 0.0
    private var frequency = 440.0
    private var detune = 0.0
    private val rainbow = Color.rainbow(256)
    private val maxShapes = numShapes.coerceIn(3, 6) // Limit recursion depth

    fun setFrequency(newFrequency: Double) {
        frequency = newFrequency
    }

    fun setDetune(newDetune: Double) {
        detune = newDetune
    }

    override fun update() {
        time += 0.01 // Slow down the update speed for smoother animation
    }

    override fun draw() {
        drawSacredGeometry(
            Vec3(centerX, centerY, 0.0),
            initialRadius = width / 4.0,
            layer = 0
        )
    }

    private fun drawSacredGeometry(center: Vec3, initialRadius: Double, layer: Int) {
        if (layer >= maxShapes || initialRadius < 10.0) return // Skip too-small shapes or deep recursion

        val dynamicRadius = initialRadius * (1 + 0.1 * sin(time + layer * detune * 0.01))
        val numPoints = 6 + (layer % 2) * 4 // Fewer points for deeper layers

        useGeometryContext {
            for (i in 0 until numPoints step 2) { // Skip some lines for less density
                val angle = 2 * PI * i / numPoints
                val point3D = Vec3(
                    center.x + dynamicRadius * cos(angle),
                    center.y + dynamicRadius * sin(angle),
                    sin(layer + time) * dynamicRadius // Add 3D depth
                )

                // Apply 3D rotations
                val rotated = rotateZ(rotateY(rotateX(point3D, frequency * 0.0005), detune * 0.005), time * 0.02)

                // Project to 2D
                val projected = projectTo2D(rotated, width.toDouble(), height.toDouble())

                // Use dynamic colors
                val hueShift = ((time * 20 + i * 10 + layer * frequency * 0.1) % 360).toFloat()
                val color = Color.applyHueShift(rainbow[i % rainbow.size], hueShift)

                drawLine(center.x, center.y, projected.x, projected.y, color.r, color.g, color.b, 0xAAu)

                // Recursively draw smaller shapes
                drawSacredGeometry(point3D, dynamicRadius / 2.0, layer + 1)
            }
        }
    }

    private fun rotateX(point: Vec3, angle: Double): Vec3 {
        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        return Vec3(
            point.x,
            point.y * cosTheta - point.z * sinTheta,
            point.y * sinTheta + point.z * cosTheta
        )
    }

    private fun rotateY(point: Vec3, angle: Double): Vec3 {
        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        return Vec3(
            point.x * cosTheta + point.z * sinTheta,
            point.y,
            -point.x * sinTheta + point.z * cosTheta
        )
    }

    private fun rotateZ(point: Vec3, angle: Double): Vec3 {
        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        return Vec3(
            point.x * cosTheta - point.y * sinTheta,
            point.x * sinTheta + point.y * cosTheta,
            point.z
        )
    }

    private fun projectTo2D(point: Vec3, screenWidth: Double, screenHeight: Double): Vec2 {
        val perspective = 500.0 // Adjust for zoom
        val scale = perspective / (perspective + point.z)
        return Vec2(
            screenWidth / 2 + point.x * scale,
            screenHeight / 2 - point.y * scale
        )
    }
}
