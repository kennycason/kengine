package com.kengine.three

import com.kengine.math.Vec3
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertTrue

class ThirdPersonCamera3DTest {
    @Test
    fun createsCameraBehindTargetAtDefaultYaw() {
        val settings = ThirdPersonCameraSettings3D(
            targetHeight = 0.6,
            eyeHeight = 1.0,
            distanceStops = listOf(5.0)
        )

        val camera = ThirdPersonCamera3D.fromTarget(
            target = Vec3(1.0, 2.0, 3.0),
            yawRadians = 0.0,
            pitchRadians = 0.0,
            distance = 5.0,
            settings = settings
        )

        assertClose(1.0, camera.eye.x)
        assertClose(3.0, camera.eye.y)
        assertClose(8.0, camera.eye.z)
        assertClose(1.0, camera.target.x)
        assertClose(2.6, camera.target.y)
        assertClose(3.0, camera.target.z)
    }

    @Test
    fun cyclesDistanceOnPressedEdgeOnly() {
        val controller = ThirdPersonCameraController3D(
            target = Vec3(0.0, 0.0, 0.0),
            settings = ThirdPersonCameraSettings3D(distanceStops = listOf(3.0, 5.0, 7.0))
        )

        controller.updateInput(ThirdPersonCameraInput3D(cycleDistance = true), 1.0 / 60.0)
        assertClose(5.0, controller.distance)

        controller.updateInput(ThirdPersonCameraInput3D(cycleDistance = true), 1.0 / 60.0)
        assertClose(5.0, controller.distance)

        controller.updateInput(ThirdPersonCameraInput3D(cycleDistance = false), 1.0 / 60.0)
        controller.updateInput(ThirdPersonCameraInput3D(cycleDistance = true), 1.0 / 60.0)
        assertClose(7.0, controller.distance)
    }

    @Test
    fun movementDirectionUsesCameraYaw() {
        val forward = movementDirection(
            inputRight = 0.0,
            inputForward = 1.0,
            yawRadians = PI / 2.0
        )

        assertClose(-1.0, forward.x)
        assertClose(0.0, forward.y)
        assertClose(0.0, forward.z)
    }

    @Test
    fun positiveLookYDecreasesPitchByDefault() {
        val controller = ThirdPersonCameraController3D(
            target = Vec3(0.0, 0.0, 0.0),
            pitchRadians = 0.4,
            settings = ThirdPersonCameraSettings3D(
                pitchSpeed = 1.0,
                lookSmoothing = 100.0,
                distanceStops = listOf(4.0)
            )
        )

        controller.updateInput(ThirdPersonCameraInput3D(lookY = 1.0), deltaSeconds = 0.1)

        assertClose(0.3, controller.pitchRadians)
    }

    @Test
    fun invertedLookYIncreasesPitchForPositiveLookY() {
        val controller = ThirdPersonCameraController3D(
            target = Vec3(0.0, 0.0, 0.0),
            pitchRadians = 0.4,
            settings = ThirdPersonCameraSettings3D(
                pitchSpeed = 1.0,
                lookSmoothing = 100.0,
                distanceStops = listOf(4.0),
                invertLookY = true
            )
        )

        controller.updateInput(ThirdPersonCameraInput3D(lookY = 1.0), deltaSeconds = 0.1)

        assertClose(0.5, controller.pitchRadians)
    }

    @Test
    fun followsTargetWithConfiguredSmoothing() {
        val controller = ThirdPersonCameraController3D(
            target = Vec3(0.0, 0.0, 0.0),
            settings = ThirdPersonCameraSettings3D(
                followSmoothing = 5.0,
                distanceStops = listOf(4.0)
            )
        )

        controller.follow(Vec3(10.0, 0.0, 0.0), deltaSeconds = 0.1)

        assertClose(5.0, controller.focus.x)
        assertClose(0.0, controller.focus.y)
        assertClose(0.0, controller.focus.z)
    }

    private fun assertClose(
        expected: Double,
        actual: Double,
        epsilon: Double = 0.000001
    ) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= epsilon,
            "Expected <$expected>, actual <$actual>."
        )
    }
}
