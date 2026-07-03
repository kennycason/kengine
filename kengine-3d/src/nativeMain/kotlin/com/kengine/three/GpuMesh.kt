package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import cnames.structs.SDL_GPUTransferBuffer
import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import sdl3.SDL_AcquireGPUCommandBuffer
import sdl3.SDL_BeginGPUCopyPass
import sdl3.SDL_CreateGPUBuffer
import sdl3.SDL_CreateGPUTransferBuffer
import sdl3.SDL_EndGPUCopyPass
import sdl3.SDL_GPUBufferCreateInfo
import sdl3.SDL_GPUBufferRegion
import sdl3.SDL_GPUTransferBufferCreateInfo
import sdl3.SDL_GPUTransferBufferLocation
import sdl3.SDL_GPUTransferBufferUsage
import sdl3.SDL_GPU_BUFFERUSAGE_VERTEX
import sdl3.SDL_GetError
import sdl3.SDL_MapGPUTransferBuffer
import sdl3.SDL_ReleaseGPUBuffer
import sdl3.SDL_ReleaseGPUTransferBuffer
import sdl3.SDL_SubmitGPUCommandBuffer
import sdl3.SDL_UnmapGPUTransferBuffer
import sdl3.SDL_UploadToGPUBuffer
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
            require(vertices.isNotEmpty()) {
                "GpuMesh requires at least one vertex."
            }

            val values = FloatArray(vertices.size * Vertex3D.FLOATS_PER_VERTEX)
            vertices.forEachIndexed { index, vertex ->
                vertex.writeTo(values, index * Vertex3D.FLOATS_PER_VERTEX)
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
            val byteSize = (vertexData.size * 4).toUInt()

            return memScoped {
                val bufferInfo = alloc<SDL_GPUBufferCreateInfo>()
                bufferInfo.usage = SDL_GPU_BUFFERUSAGE_VERTEX
                bufferInfo.size = byteSize
                bufferInfo.props = 0u

                val vertexBuffer = SDL_CreateGPUBuffer(gpu.device, bufferInfo.ptr)
                    ?: throw IllegalStateException("Error creating GPU vertex buffer: ${SDL_GetError()?.toKString()}")

                val transferBuffer = createTransferBuffer(gpu, byteSize)
                try {
                    uploadVertexData(gpu, vertexData, byteSize, transferBuffer, vertexBuffer)
                } catch (e: Throwable) {
                    SDL_ReleaseGPUBuffer(gpu.device, vertexBuffer)
                    throw e
                } finally {
                    SDL_ReleaseGPUTransferBuffer(gpu.device, transferBuffer)
                }

                GpuMesh(gpu, vertexBuffer, vertexCount)
            }
        }

        private fun createTransferBuffer(
            gpu: GpuContext,
            byteSize: UInt
        ): CPointer<SDL_GPUTransferBuffer> {
            return memScoped {
                val transferInfo = alloc<SDL_GPUTransferBufferCreateInfo>()
                transferInfo.usage = SDL_GPUTransferBufferUsage.SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD
                transferInfo.size = byteSize
                transferInfo.props = 0u

                SDL_CreateGPUTransferBuffer(gpu.device, transferInfo.ptr)
                    ?: throw IllegalStateException("Error creating GPU transfer buffer: ${SDL_GetError()?.toKString()}")
            }
        }

        private fun uploadVertexData(
            gpu: GpuContext,
            vertexData: FloatArray,
            byteSize: UInt,
            transferBuffer: CPointer<SDL_GPUTransferBuffer>,
            vertexBuffer: CPointer<SDL_GPUBuffer>
        ) {
            val mapped = SDL_MapGPUTransferBuffer(gpu.device, transferBuffer, false)
                ?: throw IllegalStateException("Error mapping GPU transfer buffer: ${SDL_GetError()?.toKString()}")

            vertexData.usePinned { pinned ->
                memcpy(
                    mapped,
                    pinned.addressOf(0),
                    byteSize.convert()
                )
            }
            SDL_UnmapGPUTransferBuffer(gpu.device, transferBuffer)

            memScoped {
                val commandBuffer = SDL_AcquireGPUCommandBuffer(gpu.device)
                    ?: throw IllegalStateException("Error acquiring GPU command buffer: ${SDL_GetError()?.toKString()}")
                val copyPass = SDL_BeginGPUCopyPass(commandBuffer)
                    ?: throw IllegalStateException("Error beginning GPU copy pass: ${SDL_GetError()?.toKString()}")

                val source = alloc<SDL_GPUTransferBufferLocation>()
                source.transfer_buffer = transferBuffer
                source.offset = 0u

                val destination = alloc<SDL_GPUBufferRegion>()
                destination.buffer = vertexBuffer
                destination.offset = 0u
                destination.size = byteSize

                SDL_UploadToGPUBuffer(copyPass, source.ptr, destination.ptr, false)
                SDL_EndGPUCopyPass(copyPass)

                if (!SDL_SubmitGPUCommandBuffer(commandBuffer)) {
                    throw IllegalStateException("Error submitting GPU upload command buffer: ${SDL_GetError()?.toKString()}")
                }
            }
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
