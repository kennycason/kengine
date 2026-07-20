package com.kengine.input.controller

import kotlin.math.abs

data class ControllerAxisInputSettings(
    val axisCount: Int = 6,
    val deadzone: Double = 0.14,
    val rawReleaseDeadzone: Float = 0.08f,
    val calibrationSeconds: Double = 0.2
) {
    init {
        require(axisCount >= 0) {
            "Controller axis count must be non-negative."
        }
        require(deadzone >= 0.0 && deadzone < 1.0) {
            "Controller deadzone must be in [0, 1)."
        }
        require(rawReleaseDeadzone >= 0f && rawReleaseDeadzone < 1f) {
            "Controller raw release deadzone must be in [0, 1)."
        }
        require(calibrationSeconds >= 0.0) {
            "Controller calibration seconds must be non-negative."
        }
    }

    fun shapeAxis(value: Float): Double {
        val raw = value.toDouble().coerceIn(-1.0, 1.0)
        val magnitude = abs(raw)
        if (magnitude < deadzone) {
            return 0.0
        }

        val normalized = ((magnitude - deadzone) / (1.0 - deadzone)).coerceIn(0.0, 1.0)
        return if (raw < 0.0) -normalized else normalized
    }

    internal fun adjustedRawAxis(
        rawValue: Float,
        neutralValue: Float,
        isCalibrating: Boolean
    ): Float {
        if (isCalibrating || abs(rawValue) < rawReleaseDeadzone) {
            return 0f
        }

        val adjustedValue = (rawValue - neutralValue).coerceIn(-1f, 1f)
        return if (abs(adjustedValue) < rawReleaseDeadzone) 0f else adjustedValue
    }
}

class ControllerAxisSample internal constructor(
    val controllerId: UInt?,
    val isCalibrating: Boolean,
    private val axes: DoubleArray
) {
    val hasController: Boolean
        get() = controllerId != null

    fun axis(
        axisIndex: Int,
        invert: Boolean = false
    ): Double {
        val value = axes.getOrElse(axisIndex) { 0.0 }
        return if (invert) -value else value
    }
}

class CalibratedControllerAxes(
    val settings: ControllerAxisInputSettings = ControllerAxisInputSettings()
) {
    private var calibratedControllerId: UInt? = null
    private var controllerNeutral: FloatArray? = null
    private var calibrationUntil = 0.0

    fun sample(
        controller: ControllerInputEventSubscriber,
        elapsedSeconds: Double
    ): ControllerAxisSample {
        return sample(
            controllerId = controller.getFirstControllerId(),
            elapsedSeconds = elapsedSeconds
        ) { controllerId, axisIndex ->
            controller.getAxisValue(controllerId, axisIndex)
        }
    }

    fun sample(
        controllerId: UInt?,
        elapsedSeconds: Double,
        axisValue: (UInt, Int) -> Float
    ): ControllerAxisSample {
        if (controllerId == null) {
            reset()
            return ControllerAxisSample(
                controllerId = null,
                isCalibrating = false,
                axes = DoubleArray(settings.axisCount)
            )
        }

        if (controllerId != calibratedControllerId) {
            calibratedControllerId = controllerId
            controllerNeutral = captureNeutral(controllerId, axisValue)
            calibrationUntil = elapsedSeconds + settings.calibrationSeconds
        }

        val isCalibrating = elapsedSeconds <= calibrationUntil
        val neutral = controllerNeutral ?: captureNeutral(controllerId, axisValue).also {
            controllerNeutral = it
        }
        val axes = DoubleArray(settings.axisCount) { axisIndex ->
            settings.shapeAxis(
                settings.adjustedRawAxis(
                    rawValue = axisValue(controllerId, axisIndex),
                    neutralValue = neutral.getOrElse(axisIndex) { 0f },
                    isCalibrating = isCalibrating
                )
            )
        }

        return ControllerAxisSample(
            controllerId = controllerId,
            isCalibrating = isCalibrating,
            axes = axes
        )
    }

    fun reset() {
        calibratedControllerId = null
        controllerNeutral = null
        calibrationUntil = 0.0
    }

    private fun captureNeutral(
        controllerId: UInt,
        axisValue: (UInt, Int) -> Float
    ): FloatArray {
        return FloatArray(settings.axisCount) { axisIndex ->
            axisValue(controllerId, axisIndex)
        }
    }
}
