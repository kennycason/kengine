package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUBuffer

@OptIn(ExperimentalForeignApi::class)
class LitGpuMesh private constructor(
    private val gpu: GpuContext,
    val vertexBuffer: CPointer<SDL_GPUBuffer>,
    val vertexCount: UInt
) {
    private var cleanedUp = false

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
            vertices: List<LitVertex3D>
        ): LitGpuMesh {
            val values = packVertexData3D(
                vertices = vertices,
                floatsPerVertex = LitVertex3D.FLOATS_PER_VERTEX,
                label = "LitGpuMesh"
            ) { vertex, packed, offset ->
                vertex.writeTo(packed, offset)
            }

            return create(gpu, values, vertices.size.toUInt())
        }

        private fun create(
            gpu: GpuContext,
            vertexData: FloatArray,
            vertexCount: UInt
        ): LitGpuMesh {
            val buffer = gpu.createVertexBuffer3D(vertexData, vertexCount, label = "lit GPU")
            return LitGpuMesh(gpu, buffer.buffer, buffer.vertexCount)
        }
    }
}
