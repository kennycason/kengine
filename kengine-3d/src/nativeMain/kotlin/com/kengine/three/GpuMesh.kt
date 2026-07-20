package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class CubeFaceColors(
    val negativeZ: Color,
    val positiveZ: Color,
    val negativeX: Color,
    val positiveX: Color,
    val positiveY: Color,
    val negativeY: Color
) {
    companion object {
        fun default(): CubeFaceColors {
            return CubeFaceColors(
                negativeZ = Color.fromHex("ff4058"),
                positiveZ = Color.fromHex("35d08b"),
                negativeX = Color.fromHex("2e6df6"),
                positiveX = Color.fromHex("f0c84b"),
                positiveY = Color.fromHex("35c9d0"),
                negativeY = Color.fromHex("c05cff")
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class GpuMesh private constructor(
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
            vertices: List<Vertex3D>
        ): GpuMesh {
            val values = packVertexData3D(
                vertices = vertices,
                floatsPerVertex = Vertex3D.FLOATS_PER_VERTEX,
                label = "GpuMesh"
            ) { vertex, packed, offset ->
                vertex.writeTo(packed, offset)
            }

            return create(gpu, values, vertices.size.toUInt())
        }

        fun cube(
            gpu: GpuContext,
            colors: CubeFaceColors = CubeFaceColors.default()
        ): GpuMesh {
            return create(gpu, cubeVertices(colors))
        }

        fun sphere(
            gpu: GpuContext,
            radius: Double = 0.5,
            color: Color = Color.fromHex("f0c84b"),
            rings: Int = 8,
            segments: Int = 12
        ): GpuMesh {
            require(rings >= 3) {
                "Sphere requires at least 3 rings."
            }
            require(segments >= 4) {
                "Sphere requires at least 4 segments."
            }

            return create(gpu, sphereVertices(radius, color, rings, segments))
        }

        private fun create(
            gpu: GpuContext,
            vertexData: FloatArray,
            vertexCount: UInt
        ): GpuMesh {
            val buffer = gpu.createVertexBuffer3D(vertexData, vertexCount, label = "GPU")
            return GpuMesh(gpu, buffer.buffer, buffer.vertexCount)
        }

        private fun cubeVertices(colors: CubeFaceColors): List<Vertex3D> {
            fun v(x: Double, y: Double, z: Double, color: Color): Vertex3D {
                return Vertex3D(Vec3(x, y, z), color)
            }

            return listOf(
                v(-0.5, -0.5, -0.5, colors.negativeZ),
                v(-0.5, 0.5, -0.5, colors.negativeZ),
                v(0.5, 0.5, -0.5, colors.negativeZ),
                v(-0.5, -0.5, -0.5, colors.negativeZ),
                v(0.5, 0.5, -0.5, colors.negativeZ),
                v(0.5, -0.5, -0.5, colors.negativeZ),

                v(0.5, -0.5, 0.5, colors.positiveZ),
                v(0.5, 0.5, 0.5, colors.positiveZ),
                v(-0.5, 0.5, 0.5, colors.positiveZ),
                v(0.5, -0.5, 0.5, colors.positiveZ),
                v(-0.5, 0.5, 0.5, colors.positiveZ),
                v(-0.5, -0.5, 0.5, colors.positiveZ),

                v(-0.5, -0.5, 0.5, colors.negativeX),
                v(-0.5, 0.5, 0.5, colors.negativeX),
                v(-0.5, 0.5, -0.5, colors.negativeX),
                v(-0.5, -0.5, 0.5, colors.negativeX),
                v(-0.5, 0.5, -0.5, colors.negativeX),
                v(-0.5, -0.5, -0.5, colors.negativeX),

                v(0.5, -0.5, -0.5, colors.positiveX),
                v(0.5, 0.5, -0.5, colors.positiveX),
                v(0.5, 0.5, 0.5, colors.positiveX),
                v(0.5, -0.5, -0.5, colors.positiveX),
                v(0.5, 0.5, 0.5, colors.positiveX),
                v(0.5, -0.5, 0.5, colors.positiveX),

                v(-0.5, 0.5, -0.5, colors.positiveY),
                v(-0.5, 0.5, 0.5, colors.positiveY),
                v(0.5, 0.5, 0.5, colors.positiveY),
                v(-0.5, 0.5, -0.5, colors.positiveY),
                v(0.5, 0.5, 0.5, colors.positiveY),
                v(0.5, 0.5, -0.5, colors.positiveY),

                v(-0.5, -0.5, 0.5, colors.negativeY),
                v(-0.5, -0.5, -0.5, colors.negativeY),
                v(0.5, -0.5, -0.5, colors.negativeY),
                v(-0.5, -0.5, 0.5, colors.negativeY),
                v(0.5, -0.5, -0.5, colors.negativeY),
                v(0.5, -0.5, 0.5, colors.negativeY)
            )
        }

        private fun sphereVertices(
            radius: Double,
            color: Color,
            rings: Int,
            segments: Int
        ): List<Vertex3D> {
            fun point(ring: Int, segment: Int): Vec3 {
                val v = ring.toDouble() / rings.toDouble()
                val phi = v * PI
                val y = cos(phi)
                val ringRadius = sin(phi)
                val theta = (segment.toDouble() / segments.toDouble()) * PI * 2.0
                return Vec3(
                    cos(theta) * ringRadius * radius,
                    y * radius,
                    sin(theta) * ringRadius * radius
                )
            }

            val vertices = mutableListOf<Vertex3D>()
            for (ring in 0 until rings) {
                for (segment in 0 until segments) {
                    val nextRing = ring + 1
                    val nextSegment = (segment + 1) % segments
                    val a = point(ring, segment)
                    val b = point(nextRing, segment)
                    val c = point(nextRing, nextSegment)
                    val d = point(ring, nextSegment)

                    vertices += Vertex3D(a, color)
                    vertices += Vertex3D(b, color)
                    vertices += Vertex3D(c, color)

                    vertices += Vertex3D(a, color)
                    vertices += Vertex3D(c, color)
                    vertices += Vertex3D(d, color)
                }
            }
            return vertices
        }
    }
}
