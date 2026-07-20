package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUBuffer

@OptIn(ExperimentalForeignApi::class)
class SkinnedTexturedLitGpuMesh private constructor(
    private val gpu: GpuContext,
    val vertexBuffer: CPointer<SDL_GPUBuffer>,
    val vertexCount: UInt,
    val maxJointIndex: Int
) : GpuResource3D {
    private var cleanedUp = false

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        SDL_ReleaseGPUBuffer(gpu.device, vertexBuffer)
    }

    companion object {
        fun create(
            gpu: GpuContext,
            vertices: List<SkinnedTexturedLitVertex3D>
        ): SkinnedTexturedLitGpuMesh {
            var maxJointIndex = 0
            val values = packVertexData3D(
                vertices = vertices,
                floatsPerVertex = SkinnedTexturedLitVertex3D.FLOATS_PER_VERTEX,
                label = "SkinnedTexturedLitGpuMesh"
            ) { vertex, packed, offset ->
                vertex.writeTo(packed, offset)
                maxJointIndex = maxOf(maxJointIndex, vertex.maxWeightedJointIndex())
            }

            return create(gpu, values, vertices.size.toUInt(), maxJointIndex)
        }

        private fun create(
            gpu: GpuContext,
            vertexData: FloatArray,
            vertexCount: UInt,
            maxJointIndex: Int
        ): SkinnedTexturedLitGpuMesh {
            val buffer = gpu.createVertexBuffer3D(vertexData, vertexCount, label = "skinned textured lit GPU")
            return SkinnedTexturedLitGpuMesh(gpu, buffer.buffer, buffer.vertexCount, maxJointIndex)
        }
    }
}
