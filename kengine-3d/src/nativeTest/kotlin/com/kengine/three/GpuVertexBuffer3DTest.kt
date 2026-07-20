package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GpuVertexBuffer3DTest {
    @Test
    fun packVertexDataWritesVerticesAtExpectedOffsets() {
        val offsets = mutableListOf<Int>()
        val vertices = listOf(
            TestVertex(1f, 2f, 3f),
            TestVertex(4f, 5f, 6f)
        )

        val values = packVertexData3D(
            vertices = vertices,
            floatsPerVertex = 3,
            label = "TestMesh"
        ) { vertex, packed, offset ->
            offsets += offset
            packed[offset] = vertex.x
            packed[offset + 1] = vertex.y
            packed[offset + 2] = vertex.z
        }

        assertEquals(listOf(0, 3), offsets)
        assertContentEquals(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f), values)
    }

    @Test
    fun packVertexDataRejectsEmptyVertexLists() {
        val error = assertFailsWith<IllegalArgumentException> {
            packVertexData3D(
                vertices = emptyList<TestVertex>(),
                floatsPerVertex = 3,
                label = "TestMesh"
            ) { _, _, _ ->
            }
        }

        assertEquals("TestMesh requires at least one vertex.", error.message)
    }

    @Test
    fun packVertexDataRejectsNonPositiveVertexStride() {
        val error = assertFailsWith<IllegalArgumentException> {
            packVertexData3D(
                vertices = listOf(TestVertex(1f, 2f, 3f)),
                floatsPerVertex = 0,
                label = "TestMesh"
            ) { _, _, _ ->
            }
        }

        assertEquals("TestMesh floats per vertex must be greater than zero.", error.message)
    }

    private data class TestVertex(
        val x: Float,
        val y: Float,
        val z: Float
    )
}
