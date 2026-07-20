package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import com.kengine.math.Vec3
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUBuffer

@OptIn(ExperimentalForeignApi::class)
class TexturedGpuMesh private constructor(
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
            vertices: List<TextureVertex3D>
        ): TexturedGpuMesh {
            val values = packVertexData3D(
                vertices = vertices,
                floatsPerVertex = TextureVertex3D.FLOATS_PER_VERTEX,
                label = "TexturedGpuMesh"
            ) { vertex, packed, offset ->
                vertex.writeTo(packed, offset)
            }

            return create(gpu, values, vertices.size.toUInt())
        }

        fun cube(gpu: GpuContext): TexturedGpuMesh {
            return create(gpu, cubeVertices())
        }

        fun quad(gpu: GpuContext): TexturedGpuMesh {
            return create(
                gpu,
                listOf(
                    v(-0.5, -0.5, 0.0, 0f, 1f),
                    v(-0.5, 0.5, 0.0, 0f, 0f),
                    v(0.5, 0.5, 0.0, 1f, 0f),
                    v(-0.5, -0.5, 0.0, 0f, 1f),
                    v(0.5, 0.5, 0.0, 1f, 0f),
                    v(0.5, -0.5, 0.0, 1f, 1f)
                )
            )
        }

        private fun create(
            gpu: GpuContext,
            vertexData: FloatArray,
            vertexCount: UInt
        ): TexturedGpuMesh {
            val buffer = gpu.createVertexBuffer3D(vertexData, vertexCount, label = "textured GPU")
            return TexturedGpuMesh(gpu, buffer.buffer, buffer.vertexCount)
        }

        private fun cubeVertices(): List<TextureVertex3D> {
            return listOf(
                v(-0.5, -0.5, -0.5, 0f, 1f), v(-0.5, 0.5, -0.5, 0f, 0f), v(0.5, 0.5, -0.5, 1f, 0f),
                v(-0.5, -0.5, -0.5, 0f, 1f), v(0.5, 0.5, -0.5, 1f, 0f), v(0.5, -0.5, -0.5, 1f, 1f),

                v(0.5, -0.5, 0.5, 0f, 1f), v(0.5, 0.5, 0.5, 0f, 0f), v(-0.5, 0.5, 0.5, 1f, 0f),
                v(0.5, -0.5, 0.5, 0f, 1f), v(-0.5, 0.5, 0.5, 1f, 0f), v(-0.5, -0.5, 0.5, 1f, 1f),

                v(-0.5, -0.5, 0.5, 0f, 1f), v(-0.5, 0.5, 0.5, 0f, 0f), v(-0.5, 0.5, -0.5, 1f, 0f),
                v(-0.5, -0.5, 0.5, 0f, 1f), v(-0.5, 0.5, -0.5, 1f, 0f), v(-0.5, -0.5, -0.5, 1f, 1f),

                v(0.5, -0.5, -0.5, 0f, 1f), v(0.5, 0.5, -0.5, 0f, 0f), v(0.5, 0.5, 0.5, 1f, 0f),
                v(0.5, -0.5, -0.5, 0f, 1f), v(0.5, 0.5, 0.5, 1f, 0f), v(0.5, -0.5, 0.5, 1f, 1f),

                v(-0.5, 0.5, -0.5, 0f, 1f), v(-0.5, 0.5, 0.5, 0f, 0f), v(0.5, 0.5, 0.5, 1f, 0f),
                v(-0.5, 0.5, -0.5, 0f, 1f), v(0.5, 0.5, 0.5, 1f, 0f), v(0.5, 0.5, -0.5, 1f, 1f),

                v(-0.5, -0.5, 0.5, 0f, 1f), v(-0.5, -0.5, -0.5, 0f, 0f), v(0.5, -0.5, -0.5, 1f, 0f),
                v(-0.5, -0.5, 0.5, 0f, 1f), v(0.5, -0.5, -0.5, 1f, 0f), v(0.5, -0.5, 0.5, 1f, 1f)
            )
        }

        private fun v(x: Double, y: Double, z: Double, u: Float, v: Float): TextureVertex3D {
            return TextureVertex3D(Vec3(x, y, z), u, v)
        }
    }
}
