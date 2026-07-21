package com.kengine.input.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalibratedControllerAxesTest {
    @Test
    fun samplesZeroAxesWithoutController() {
        val axes = CalibratedControllerAxes(settings = testSettings())

        val sample = axes.sample(
            controllerId = null,
            elapsedSeconds = 1.0,
            axisValue = { _, _ -> 1f }
        )

        assertFalse(sample.hasController)
        assertFalse(sample.isCalibrating)
        assertEquals(0.0, sample.axis(0))
    }

    @Test
    fun calibratesAgainstInitialNeutralAxisValues() {
        val axes = CalibratedControllerAxes(settings = testSettings())
        var axisValues = floatArrayOf(0.1f, -0.1f)

        val calibrating = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.0
        ) { _, axisIndex ->
            axisValues[axisIndex]
        }

        assertTrue(calibrating.hasController)
        assertTrue(calibrating.isCalibrating)
        assertEquals(0.0, calibrating.axis(0))

        axisValues = floatArrayOf(0.57f, -0.1f)
        val active = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.31
        ) { _, axisIndex ->
            axisValues[axisIndex]
        }

        assertFalse(active.isCalibrating)
        assertEquals(0.38372093023255816, active.axis(0), absoluteTolerance = 0.000001)
        assertEquals(0.0, active.axis(1))
    }

    @Test
    fun supportsInvertedAxisReadsAndMissingAxes() {
        val axes = CalibratedControllerAxes(settings = testSettings(axisCount = 1, calibrationSeconds = 0.0))

        axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.0
        ) { _, _ ->
            0f
        }
        val sample = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.01
        ) { _, _ ->
            0.57f
        }

        assertEquals(0.5, sample.axis(0), absoluteTolerance = 0.000001)
        assertEquals(-0.5, sample.axis(0, invert = true), absoluteTolerance = 0.000001)
        assertEquals(0.0, sample.axis(12))
    }

    @Test
    fun resetsCalibrationWhenControllerDisconnects() {
        val axes = CalibratedControllerAxes(settings = testSettings(calibrationSeconds = 0.2))

        axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.0
        ) { _, _ ->
            0.2f
        }
        axes.sample(
            controllerId = null,
            elapsedSeconds = 1.1,
            axisValue = { _, _ -> 0f }
        )
        val reconnected = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.2
        ) { _, _ ->
            0.9f
        }

        assertTrue(reconnected.isCalibrating)
        assertEquals(0.0, reconnected.axis(0))
    }

    @Test
    fun rawReleaseDeadzoneAppliesBeforeAndAfterNeutralAdjustment() {
        val axes = CalibratedControllerAxes(settings = testSettings(deadzone = 0.0, rawReleaseDeadzone = 0.08f))
        var value = 0.1f

        axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.0
        ) { _, _ ->
            value
        }

        value = 0.07f
        val releasedRaw = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.31
        ) { _, _ ->
            value
        }
        assertEquals(0.0, releasedRaw.axis(0))

        value = 0.17f
        val releasedAdjusted = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.32
        ) { _, _ ->
            value
        }
        assertEquals(0.0, releasedAdjusted.axis(0))
    }

    @Test
    fun doesNotCaptureHeldStickAsNeutralCenter() {
        val axes = CalibratedControllerAxes(settings = testSettings(calibrationSeconds = 0.2))
        var value = 0.9f

        val calibrating = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.0
        ) { _, _ ->
            value
        }

        assertTrue(calibrating.isCalibrating)
        assertEquals(0.0, calibrating.axis(0))

        value = 0f
        val released = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.31
        ) { _, _ ->
            value
        }
        assertEquals(0.0, released.axis(0))

        value = 0.57f
        val active = axes.sample(
            controllerId = 1u,
            elapsedSeconds = 1.32
        ) { _, _ ->
            value
        }
        assertEquals(0.5, active.axis(0), absoluteTolerance = 0.000001)
    }

    private fun testSettings(
        axisCount: Int = 2,
        deadzone: Double = 0.14,
        rawReleaseDeadzone: Float = 0.08f,
        neutralCaptureMaxMagnitude: Float = 0.25f,
        calibrationSeconds: Double = 0.2
    ): ControllerAxisInputSettings {
        return ControllerAxisInputSettings(
            axisCount = axisCount,
            deadzone = deadzone,
            rawReleaseDeadzone = rawReleaseDeadzone,
            neutralCaptureMaxMagnitude = neutralCaptureMaxMagnitude,
            calibrationSeconds = calibrationSeconds
        )
    }
}
