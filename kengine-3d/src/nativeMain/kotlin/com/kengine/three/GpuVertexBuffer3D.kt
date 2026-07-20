package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import cnames.structs.SDL_GPUTransferBuffer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl3.SDL_CreateGPUBuffer
import sdl3.SDL_GPUBufferCreateInfo
import sdl3.SDL_GPUBufferRegion
import sdl3.SDL_GPUTransferBufferLocation
import sdl3.SDL_GPU_BUFFERUSAGE_VERTEX
import sdl3.SDL_ReleaseGPUBuffer
import sdl3.SDL_UploadToGPUBuffer

@OptIn(ExperimentalForeignApi::class)
internal data class GpuVertexBuffer3D(
    val buffer: CPointer<SDL_GPUBuffer>,
    val vertexCount: UInt
)

internal inline fun <T> packVertexData3D(
    vertices: List<T>,
    floatsPerVertex: Int,
    label: String,
    write: (vertex: T, values: FloatArray, offset: Int) -> Unit
): FloatArray {
    require(vertices.isNotEmpty()) {
        "$label requires at least one vertex."
    }
    require(floatsPerVertex > 0) {
        "$label floats per vertex must be greater than zero."
    }

    val values = FloatArray(vertices.size * floatsPerVertex)
    vertices.forEachIndexed { index, vertex ->
        write(vertex, values, index * floatsPerVertex)
    }
    return values
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.createVertexBuffer3D(
    vertexData: FloatArray,
    vertexCount: UInt,
    label: String
): GpuVertexBuffer3D {
    require(vertexData.isNotEmpty()) {
        "$label vertex data must not be empty."
    }
    require(vertexCount > 0u) {
        "$label vertex count must be greater than zero."
    }

    val byteSize = vertexData.gpuByteSize3D()
    return memScoped {
        val bufferInfo = alloc<SDL_GPUBufferCreateInfo>()
        bufferInfo.usage = SDL_GPU_BUFFERUSAGE_VERTEX
        bufferInfo.size = byteSize
        bufferInfo.props = 0u

        val vertexBuffer = SDL_CreateGPUBuffer(device, bufferInfo.ptr)
            ?: throw IllegalStateException("Error creating $label vertex buffer: ${sdlErrorMessage3D()}")

        try {
            uploadVertexData3D(
                vertexBuffer = vertexBuffer,
                vertexData = vertexData,
                label = label
            )
        } catch (e: Throwable) {
            SDL_ReleaseGPUBuffer(device, vertexBuffer)
            throw e
        }

        GpuVertexBuffer3D(
            buffer = vertexBuffer,
            vertexCount = vertexCount
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.updateVertexBuffer3D(
    vertexBuffer: CPointer<SDL_GPUBuffer>,
    vertexData: FloatArray,
    label: String
) {
    require(vertexData.isNotEmpty()) {
        "$label vertex data must not be empty."
    }

    uploadVertexData3D(
        vertexBuffer = vertexBuffer,
        vertexData = vertexData,
        label = label
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun GpuContext.uploadVertexData3D(
    vertexBuffer: CPointer<SDL_GPUBuffer>,
    vertexData: FloatArray,
    label: String
) {
    val byteSize = vertexData.gpuByteSize3D()
    withUploadTransferBuffer3D(byteSize, label) { transferBuffer ->
        copyFloatDataToTransferBuffer3D(transferBuffer, vertexData, label)
        submitVertexBufferUpload3D(transferBuffer, vertexBuffer, byteSize, label)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun GpuContext.submitVertexBufferUpload3D(
    transferBuffer: CPointer<SDL_GPUTransferBuffer>,
    vertexBuffer: CPointer<SDL_GPUBuffer>,
    byteSize: UInt,
    label: String
) {
    withGpuCopyPass3D(label) { copyPass ->
        memScoped {
            val source = alloc<SDL_GPUTransferBufferLocation>()
            source.transfer_buffer = transferBuffer
            source.offset = 0u

            val destination = alloc<SDL_GPUBufferRegion>()
            destination.buffer = vertexBuffer
            destination.offset = 0u
            destination.size = byteSize

            SDL_UploadToGPUBuffer(copyPass, source.ptr, destination.ptr, false)
        }
    }
}
