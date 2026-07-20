package com.kengine.three

import com.kengine.input.keyboard.KeyboardInputEventSubscriber
import com.kengine.input.mouse.MouseInputEventSubscriber
import com.kengine.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OrbitCameraController3D(
    target: Vec3 = Vec3(0.0, 0.0, -4.0),
    distance: Double = 4.6,
    yawRadians: Float = 0f,
    pitchRadians: Float = 0.1f,
    private val dragSensitivity: Float = 0.007f,
    private val keyboardPanSpeed: Double = 1.8,
    private val keyboardZoomSpeed: Double = 2.4,
    private val minDistance: Double = 0.2,
    private val maxDistance: Double = 8.0
) {
    private var target = target
    private var distance = distance.coerceIn(minDistance, maxDistance)
    private var yawRadians = yawRadians
    private var pitchRadians = pitchRadians
    private var lastX = 0.0
    private var lastY = 0.0
    private var dragging = false

    fun update(
        mouse: MouseInputEventSubscriber,
        keyboard: KeyboardInputEventSubscriber? = null,
        deltaSeconds: Double = 1.0 / 60.0
    ) {
        updateKeyboard(keyboard, deltaSeconds)
        updateMouse(mouse)
    }

    private fun updateKeyboard(
        keyboard: KeyboardInputEventSubscriber?,
        deltaSeconds: Double
    ) {
        if (keyboard == null) {
            return
        }

        if (keyboard.isUpPressed()) {
            distance = (distance - keyboardZoomSpeed * deltaSeconds).coerceAtLeast(minDistance)
        }
        if (keyboard.isDownPressed()) {
            distance = (distance + keyboardZoomSpeed * deltaSeconds).coerceAtMost(maxDistance)
        }

        val panDirection = when {
            keyboard.isLeftPressed() && !keyboard.isRightPressed() -> 1.0
            keyboard.isRightPressed() && !keyboard.isLeftPressed() -> -1.0
            else -> 0.0
        }
        if (panDirection != 0.0) {
            val panAmount = panDirection * keyboardPanSpeed * deltaSeconds
            val right = Vec3(
                cos(yawRadians.toDouble()),
                0.0,
                -sin(yawRadians.toDouble())
            )
            target = Vec3(
                target.x + right.x * panAmount,
                target.y,
                target.z + right.z * panAmount
            )
        }
    }

    private fun updateMouse(mouse: MouseInputEventSubscriber) {
        val cursor = mouse.cursor()
        if (mouse.wasLeftJustPressed()) {
            dragging = true
            lastX = cursor.x
            lastY = cursor.y
        }

        if (!mouse.isLeftPressed()) {
            dragging = false
            return
        }

        if (!dragging) {
            return
        }

        val dx = cursor.x - lastX
        val dy = cursor.y - lastY
        yawRadians += (dx * dragSensitivity).toFloat()
        pitchRadians = (pitchRadians + (dy * dragSensitivity).toFloat())
            .coerceIn(-MAX_PITCH, MAX_PITCH)
        lastX = cursor.x
        lastY = cursor.y
    }

    fun camera(): OrbitCamera3D {
        return OrbitCamera3D(
            target = target,
            distance = distance,
            yawRadians = yawRadians,
            pitchRadians = pitchRadians
        )
    }

    companion object {
        private const val MAX_PITCH = (PI.toFloat() * 0.42f)
    }
}
