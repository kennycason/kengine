package com.kengine.three

import sdl3.SDL_GPUPrimitiveType
import sdl3.SDL_GPUVertexElementFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GpuPipeline3DTest {
    @Test
    fun singleBufferLayoutReportsPipelineContext() {
        val layout = GpuVertexInputLayout3D.singleBuffer(
            pitchBytes = TextureVertex3D.BYTES_PER_VERTEX,
            GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0),
            GpuVertexAttribute3D.float2(location = 1, offsetBytes = 12)
        )

        assertEquals(listOf("attributes=2", "vertexBytes=20"), layout.errorContext())
    }

    @Test
    fun attributeHelpersUseExpectedFormats() {
        assertEquals(
            SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT2,
            GpuVertexAttribute3D.float2(location = 1, offsetBytes = 12).format
        )
        assertEquals(
            SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT3,
            GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0).format
        )
        assertEquals(
            SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT4,
            GpuVertexAttribute3D.float4(location = 4, offsetBytes = 44).format
        )
    }

    @Test
    fun layoutsRejectAttributesForMissingBuffers() {
        assertFailsWith<IllegalArgumentException> {
            GpuVertexInputLayout3D(
                attributes = listOf(GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0))
            )
        }
    }

    @Test
    fun layoutsRejectDuplicateBufferSlots() {
        assertFailsWith<IllegalArgumentException> {
            GpuVertexInputLayout3D(
                buffers = listOf(
                    GpuVertexBufferLayout3D(pitchBytes = 12, slot = 0),
                    GpuVertexBufferLayout3D(pitchBytes = 12, slot = 0)
                )
            )
        }
    }

    @Test
    fun layoutsRejectDuplicateAttributeLocations() {
        assertFailsWith<IllegalArgumentException> {
            GpuVertexInputLayout3D.singleBuffer(
                pitchBytes = Vertex3D.BYTES_PER_VERTEX,
                GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0),
                GpuVertexAttribute3D.float3(location = 0, offsetBytes = 12)
            )
        }
    }

    @Test
    fun layoutsRejectAttributeOffsetsOutsideVertexPitch() {
        assertFailsWith<IllegalArgumentException> {
            GpuVertexInputLayout3D.singleBuffer(
                pitchBytes = 12,
                GpuVertexAttribute3D.float3(location = 0, offsetBytes = 12)
            )
        }
    }

    @Test
    fun linePipelineDefaultsToNoDepthWrites() {
        val descriptor = GpuGraphicsPipelineDescriptor3D.lineList(
            label = "debug",
            vertexInput = GpuVertexInputLayout3D.singleBuffer(
                pitchBytes = Vertex3D.BYTES_PER_VERTEX,
                GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0)
            )
        )

        assertEquals(SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_LINELIST, descriptor.primitiveType)
        assertTrue(descriptor.enableDepthTest)
        assertFalse(descriptor.enableDepthWrite)
        assertEquals("attributes=1, vertexBytes=24", descriptor.errorContext())
    }
}
