package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SkinnedTexturedLitVertex3DTest {
    @Test
    fun writesInterleavedGpuVertexData() {
        val vertex = SkinnedTexturedLitVertex3D(
            position = Vec3(1.0, 2.0, 3.0),
            normal = Vec3(0.0, 1.0, 0.0),
            color = Color.fromHex("ff8000"),
            u = 0.25f,
            v = 0.75f,
            joints = SkinJointIndices3D(2, 4, 6, 8),
            weights = SkinJointWeights3D(0.5, 0.25, 0.125, 0.125)
        )
        val values = FloatArray(SkinnedTexturedLitVertex3D.FLOATS_PER_VERTEX)

        vertex.writeTo(values, 0)

        assertEquals(1f, values[0])
        assertEquals(2f, values[1])
        assertEquals(3f, values[2])
        assertEquals(0f, values[3])
        assertEquals(1f, values[4])
        assertEquals(0f, values[5])
        assertEquals(1f, values[6])
        assertEquals(128f / 255f, values[7])
        assertEquals(0f, values[8])
        assertEquals(0.25f, values[9])
        assertEquals(0.75f, values[10])
        assertEquals(2f, values[11])
        assertEquals(4f, values[12])
        assertEquals(6f, values[13])
        assertEquals(8f, values[14])
        assertEquals(0.5f, values[15])
        assertEquals(0.25f, values[16])
        assertEquals(0.125f, values[17])
        assertEquals(0.125f, values[18])
    }

    @Test
    fun reportsMaxWeightedJointIndex() {
        val vertex = SkinnedTexturedLitVertex3D(
            position = Vec3(0.0, 0.0, 0.0),
            normal = Vec3(0.0, 1.0, 0.0),
            color = Color.white,
            u = 0f,
            v = 0f,
            joints = SkinJointIndices3D(1, 7, 12, 30),
            weights = SkinJointWeights3D(0.5, 0.0, 0.5, 0.0)
        )

        assertEquals(12, vertex.maxWeightedJointIndex())
    }

    @Test
    fun normalizesSkinJointWeights() {
        val weights = SkinJointWeights3D(2.0, 1.0, 1.0, 0.0).normalized()

        assertEquals(0.5, weights.x)
        assertEquals(0.25, weights.y)
        assertEquals(0.25, weights.z)
        assertEquals(0.0, weights.w)
    }

    @Test
    fun rejectsNegativeSkinInputs() {
        assertFailsWith<IllegalArgumentException> {
            SkinJointIndices3D(-1, 0, 0, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            SkinJointWeights3D(-0.1, 1.0, 0.0, 0.0)
        }
    }
}
