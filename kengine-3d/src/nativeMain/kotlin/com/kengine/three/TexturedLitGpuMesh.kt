package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import cnames.structs.SDL_GPUTransferBuffer
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

@OptIn(ExperimentalForeignApi::class)
class TexturedLitGpuMesh private constructor(
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
            vertices: List<TexturedLitVertex3D>
        ): TexturedLitGpuMesh {
            require(vertices.isNotEmpty()) {
                "TexturedLitGpuMesh requires at least one vertex."
            }

            val values = FloatArray(vertices.size * TexturedLitVertex3D.FLOATS_PER_VERTEX)
            vertices.forEachIndexed { index, vertex ->
                vertex.writeTo(values, index * TexturedLitVertex3D.FLOATS_PER_VERTEX)
            }

            return create(gpu, values, vertices.size.toUInt())
        }

        private fun create(
            gpu: GpuContext,
            vertexData: FloatArray,
            vertexCount: UInt
        ): TexturedLitGpuMesh {
            val byteSize = (vertexData.size * 4).toUInt()

            return memScoped {
                val bufferInfo = alloc<SDL_GPUBufferCreateInfo>()
                bufferInfo.usage = SDL_GPU_BUFFERUSAGE_VERTEX
                bufferInfo.size = byteSize
                bufferInfo.props = 0u

                val vertexBuffer = SDL_CreateGPUBuffer(gpu.device, bufferInfo.ptr)
                    ?: throw IllegalStateException("Error creating textured lit GPU vertex buffer: ${SDL_GetError()?.toKString()}")

                val transferBuffer = createTransferBuffer(gpu, byteSize)
                try {
                    uploadVertexData(gpu, vertexData, byteSize, transferBuffer, vertexBuffer)
                } catch (e: Throwable) {
                    SDL_ReleaseGPUBuffer(gpu.device, vertexBuffer)
                    throw e
                } finally {
                    SDL_ReleaseGPUTransferBuffer(gpu.device, transferBuffer)
                }

                TexturedLitGpuMesh(gpu, vertexBuffer, vertexCount)
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
                    ?: throw IllegalStateException("Error creating textured lit GPU transfer buffer: ${SDL_GetError()?.toKString()}")
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
                ?: throw IllegalStateException("Error mapping textured lit GPU transfer buffer: ${SDL_GetError()?.toKString()}")

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
                    ?: throw IllegalStateException("Error acquiring textured lit GPU command buffer: ${SDL_GetError()?.toKString()}")
                val copyPass = SDL_BeginGPUCopyPass(commandBuffer)
                    ?: throw IllegalStateException("Error beginning textured lit GPU copy pass: ${SDL_GetError()?.toKString()}")

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
                    throw IllegalStateException("Error submitting textured lit GPU upload command buffer: ${SDL_GetError()?.toKString()}")
                }
            }
        }
    }
}
