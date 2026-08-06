package com.kengine.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class VectorTest {
    @Test
    fun vec2SupportsBasicOperations() {
        val vector = Vec2(3.0, 4.0)

        assertEquals(25.0, vector.lengthSquared)
        assertEquals(5.0, vector.length)
        assertEquals(5.0, vector.magnitude())
        assertEquals(Vec2(6.0, 8.0), vector * 2.0)
        assertEquals(Vec2(0.6, 0.8), vector.normalized())
        assertEquals(11.0, vector.dot(Vec2(1.0, 2.0)))
        assertEquals(Vec2(4.5, 6.0), 1.5 * vector)
    }

    @Test
    fun vec2RetainsMutableApi() {
        val vector = Vec2()
        vector.set(2.0)
        vector += Vec2(1.0, 2.0)
        vector *= 2.0

        assertEquals(Vec2(6.0, 8.0), vector)
        assertEquals(Vec2(7.0, 9.0), vector + 1.0)
        assertEquals(Vec2(5.0, 7.0), vector - 1.0)
        assertEquals(Vec2(3.0, 4.0), vector / 2.0)
        assertEquals(Vec2(3.0, 4.0), vector.linearInterpolate(Vec2(0.0, 0.0), 0.5))
    }

    @Test
    fun vec3SupportsCrossProduct() {
        val x = Vec3(1.0, 0.0, 0.0)
        val y = Vec3(0.0, 1.0, 0.0)

        assertEquals(Vec3(0.0, 0.0, 1.0), x.cross(y))
        assertEquals(0.0, x.dot(y))
        assertEquals(Vec3(2.0, 0.0, 0.0), 2.0 * x)
    }

    @Test
    fun vec3ProvidesRotationAndProjectionHelpers() {
        assertVec3Equals(Vec3(0.0, 0.0, 1.0), Vec3.rotateX(Vec3(0.0, 1.0, 0.0), Math.PI_HALF))
        assertVec3Equals(Vec3(0.0, 0.0, -1.0), Vec3(1.0, 0.0, 0.0).rotateY(Math.PI_HALF))
        assertVec3Equals(Vec3(0.0, 1.0, 0.0), Vec3(1.0, 0.0, 0.0).rotateZ(Math.PI_HALF))
        assertEquals(Vec2(321.0, 238.0), Vec3.projectTo2D(Vec3(1.0, 2.0, 0.0), 640.0, 480.0))
    }

    @Test
    fun floatVectorsCanConvertToDoubleVectors() {
        val vector = FloatVector3(1.0f, 2.0f, 2.0f)

        assertEquals(3.0f, vector.length)
        assertEquals(Vec3(1.0, 2.0, 2.0), vector.toVec3())
        assertEquals(Vec3(1.0, 2.0, 2.0), vector.toVector3())
        assertEquals(Vec2(1.0, 2.0), FloatVector2(1.0f, 2.0f).toVec2())
    }

    @Test
    fun pointsConvertToVectors() {
        assertEquals(Vec2(4.0, 7.0), Point2(4, 7).toVec2())
        assertEquals(Vec2(4.0, 7.0), Point2(4, 7).toVector2())
        assertEquals(Vec3(4.0, 7.0, 9.0), Point3(4, 7, 9).toVec3())
        assertEquals(Vec3(4.0, 7.0, 9.0), Point3(4, 7, 9).toVector3())
    }

    @Test
    fun vectorAliasesStayAvailable() {
        val vector2: Vector2 = Vec2(1.0, 2.0)
        val vector3: Vector3 = Vec3(1.0, 2.0, 3.0)
        val fVec2: FVec2 = FloatVector2(1.0f, 2.0f)
        val fVec3: FVec3 = FloatVector3(1.0f, 2.0f, 3.0f)

        assertEquals(Vec2(1.0, 2.0), vector2)
        assertEquals(Vec3(1.0, 2.0, 3.0), vector3)
        assertEquals(Vec2(0.0, 0.0), Vector2.ZERO)
        assertEquals(Vec3(0.0, 0.0, 0.0), Vector3.ZERO)
        assertEquals(FloatVector2(1.0f, 2.0f), fVec2)
        assertEquals(FloatVector3(1.0f, 2.0f, 3.0f), fVec3)
    }

    private fun assertVec3Equals(expected: Vec3, actual: Vec3, epsilon: Double = 0.000001) {
        assertTrue(abs(expected.x - actual.x) <= epsilon)
        assertTrue(abs(expected.y - actual.y) <= epsilon)
        assertTrue(abs(expected.z - actual.z) <= epsilon)
    }
}
