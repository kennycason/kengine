package com.kengine.three

import com.kengine.math.Vec3
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertTrue

class Transform3DTest {
    @Test
    fun transformPointAppliesScaleRotationAndTranslation() {
        val transform = Transform3D(
            position = Vec3(10.0, 2.0, 0.0),
            rotation = Vec3(0.0, 0.0, PI / 2.0),
            scale = Vec3(2.0, 1.0, 1.0)
        )

        val point = transform.transformPoint(Vec3(1.0, 0.0, 0.0))

        assertClose(10.0, point.x)
        assertClose(4.0, point.y)
        assertClose(0.0, point.z)
    }

    @Test
    fun combinesParentAndChildTransforms() {
        val parent = Transform3D(
            position = Vec3(10.0, 2.0, 0.0),
            rotation = Vec3(0.0, PI / 2.0, 0.0),
            scale = Vec3(2.0, 3.0, 4.0)
        )
        val child = Transform3D(
            position = Vec3(1.0, 2.0, 0.0),
            rotation = Vec3(0.1, 0.2, 0.3),
            scale = Vec3(0.5, 2.0, 1.0)
        )

        val combined = parent * child

        assertClose(10.0, combined.position.x)
        assertClose(8.0, combined.position.y)
        assertClose(-2.0, combined.position.z)
        assertClose(0.1, combined.rotation.x)
        assertClose(PI / 2.0 + 0.2, combined.rotation.y)
        assertClose(0.3, combined.rotation.z)
        assertClose(1.0, combined.scale.x)
        assertClose(6.0, combined.scale.y)
        assertClose(4.0, combined.scale.z)
    }

    @Test
    fun createsPositionYawTransformWithOffsets() {
        val transform = Transform3D.positionYaw(
            position = Vec3(1.0, 2.0, 3.0),
            yawRadians = 0.5,
            yOffset = -0.25,
            yawOffsetRadians = PI,
            scale = Vec3(2.0, 2.0, 2.0)
        )

        assertClose(1.0, transform.position.x)
        assertClose(1.75, transform.position.y)
        assertClose(3.0, transform.position.z)
        assertClose(0.0, transform.rotation.x)
        assertClose(PI + 0.5, transform.rotation.y)
        assertClose(0.0, transform.rotation.z)
        assertClose(2.0, transform.scale.x)
        assertClose(2.0, transform.scale.y)
        assertClose(2.0, transform.scale.z)
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
