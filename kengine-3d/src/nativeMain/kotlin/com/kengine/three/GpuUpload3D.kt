package com.kengine.three

import cnames.structs.SDL_GPUCopyPass
import cnames.structs.SDL_GPUTransferBuffer
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import sdl3.SDL_AcquireGPUCommandBuffer
import sdl3.SDL_BeginGPUCopyPass
import sdl3.SDL_CreateGPUTransferBuffer
import sdl3.SDL_EndGPUCopyPass
import sdl3.SDL_GPUTransferBufferCreateInfo
import sdl3.SDL_GPUTransferBufferUsage
import sdl3.SDL_MapGPUTransferBuffer
import sdl3.SDL_ReleaseGPUTransferBuffer
import sdl3.SDL_SubmitGPUCommandBuffer
import sdl3.SDL_UnmapGPUTransferBuffer

internal fun FloatArray.gpuByteSize3D(): UInt {
    return (size * Float.SIZE_BYTES).toUInt()
}

internal fun ByteArray.gpuByteSize3D(): UInt {
    return size.toUInt()
}

@OptIn(ExperimentalForeignApi::class)
internal inline fun <T> GpuContext.withUploadTransferBuffer3D(
    byteSize: UInt,
    label: String,
    block: (CPointer<SDL_GPUTransferBuffer>) -> T
): T {
    require(byteSize > 0u) {
        "$label upload byte size must be greater than zero."
    }

    val transferBuffer = createUploadTransferBuffer3D(byteSize, label)
    try {
        return block(transferBuffer)
    } finally {
        SDL_ReleaseGPUTransferBuffer(device, transferBuffer)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.copyFloatDataToTransferBuffer3D(
    transferBuffer: CPointer<SDL_GPUTransferBuffer>,
    values: FloatArray,
    label: String
) {
    require(values.isNotEmpty()) {
        "$label float upload data must not be empty."
    }

    copyDataToTransferBuffer3D(
        transferBuffer = transferBuffer,
        byteSize = values.gpuByteSize3D(),
        label = label
    ) { mapped, byteSize ->
        values.usePinned { pinned ->
            memcpy(
                mapped,
                pinned.addressOf(0),
                byteSize.convert()
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.copyByteDataToTransferBuffer3D(
    transferBuffer: CPointer<SDL_GPUTransferBuffer>,
    bytes: ByteArray,
    label: String
) {
    require(bytes.isNotEmpty()) {
        "$label byte upload data must not be empty."
    }

    copyDataToTransferBuffer3D(
        transferBuffer = transferBuffer,
        byteSize = bytes.gpuByteSize3D(),
        label = label
    ) { mapped, byteSize ->
        bytes.usePinned { pinned ->
            memcpy(
                mapped,
                pinned.addressOf(0),
                byteSize.convert()
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal inline fun GpuContext.withGpuCopyPass3D(
    label: String,
    block: (CPointer<SDL_GPUCopyPass>) -> Unit
) {
    memScoped {
        val commandBuffer = SDL_AcquireGPUCommandBuffer(device)
            ?: throw IllegalStateException("Error acquiring $label upload command buffer: ${sdlErrorMessage3D()}")
        val copyPass = SDL_BeginGPUCopyPass(commandBuffer)
            ?: throw IllegalStateException("Error beginning $label copy pass: ${sdlErrorMessage3D()}")

        try {
            block(copyPass)
        } finally {
            SDL_EndGPUCopyPass(copyPass)
        }

        if (!SDL_SubmitGPUCommandBuffer(commandBuffer)) {
            throw IllegalStateException("Error submitting $label upload command buffer: ${sdlErrorMessage3D()}")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun GpuContext.createUploadTransferBuffer3D(
    byteSize: UInt,
    label: String
): CPointer<SDL_GPUTransferBuffer> {
    return memScoped {
        val transferInfo = alloc<SDL_GPUTransferBufferCreateInfo>()
        transferInfo.usage = SDL_GPUTransferBufferUsage.SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD
        transferInfo.size = byteSize
        transferInfo.props = 0u

        SDL_CreateGPUTransferBuffer(device, transferInfo.ptr)
            ?: throw IllegalStateException("Error creating $label transfer buffer: ${sdlErrorMessage3D()}")
    }
}

@OptIn(ExperimentalForeignApi::class)
private inline fun GpuContext.copyDataToTransferBuffer3D(
    transferBuffer: CPointer<SDL_GPUTransferBuffer>,
    byteSize: UInt,
    label: String,
    copy: (mapped: COpaquePointer, byteSize: UInt) -> Unit
) {
    val mapped = SDL_MapGPUTransferBuffer(device, transferBuffer, false)
        ?: throw IllegalStateException("Error mapping $label transfer buffer: ${sdlErrorMessage3D()}")

    try {
        copy(mapped, byteSize)
    } finally {
        SDL_UnmapGPUTransferBuffer(device, transferBuffer)
    }
}
