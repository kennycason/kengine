package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUBuffer

@OptIn(ExperimentalForeignApi::class)
class TexturedLitGpuMesh private constructor(
    private val gpu: GpuContext,
    val vertexBuffer: CPointer<SDL_GPUBuffer>,
    val vertexCount: UInt
) {
    private var cleanedUp = false

    fun update(vertices: List<TexturedLitVertex3D>) {
        check(!cleanedUp) {
            "TexturedLitGpuMesh has already been cleaned up."
        }
        require(vertices.size.toUInt() == vertexCount) {
            "TexturedLitGpuMesh updates must keep the original vertex count."
        }

        val values = packVertexData3D(
            vertices = vertices,
            floatsPerVertex = TexturedLitVertex3D.FLOATS_PER_VERTEX,
            label = "TexturedLitGpuMesh"
        ) { vertex, packed, offset ->
            vertex.writeTo(packed, offset)
        }
        gpu.updateVertexBuffer3D(vertexBuffer, values, label = "textured lit GPU")
    }

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        SDL_ReleaseGPUBuffer(gpu.device, vertexBuffer)
    }

    companion object {
        fun create(
            gpu: GpuContext,
            vertices: List<TexturedLitVertex3D>
        ): TexturedLitGpuMesh {
            val values = packVertexData3D(
                vertices = vertices,
                floatsPerVertex = TexturedLitVertex3D.FLOATS_PER_VERTEX,
                label = "TexturedLitGpuMesh"
            ) { vertex, packed, offset ->
                vertex.writeTo(packed, offset)
            }

            return create(gpu, values, vertices.size.toUInt())
        }

        private fun create(
            gpu: GpuContext,
            vertexData: FloatArray,
            vertexCount: UInt
        ): TexturedLitGpuMesh {
            val buffer = gpu.createVertexBuffer3D(vertexData, vertexCount, label = "textured lit GPU")
            return TexturedLitGpuMesh(gpu, buffer.buffer, buffer.vertexCount)
        }
    }
}
