package com.kengine.three

import cnames.structs.SDL_GPUSampler
import cnames.structs.SDL_GPUTexture
import cnames.structs.SDL_GPUTransferBuffer
import com.kengine.file.File
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import sdl3.SDL_AcquireGPUCommandBuffer
import sdl3.SDL_BeginGPUCopyPass
import sdl3.SDL_CreateGPUSampler
import sdl3.SDL_CreateGPUTexture
import sdl3.SDL_CreateGPUTransferBuffer
import sdl3.SDL_EndGPUCopyPass
import sdl3.SDL_GPUCompareOp
import sdl3.SDL_GPUFilter
import sdl3.SDL_GPUSampleCount
import sdl3.SDL_GPUSamplerAddressMode
import sdl3.SDL_GPUSamplerCreateInfo
import sdl3.SDL_GPUSamplerMipmapMode
import sdl3.SDL_GPUTextureCreateInfo
import sdl3.SDL_GPUTextureFormat
import sdl3.SDL_GPUTextureRegion
import sdl3.SDL_GPUTextureTransferInfo
import sdl3.SDL_GPUTextureType
import sdl3.SDL_GPUTransferBufferCreateInfo
import sdl3.SDL_GPUTransferBufferUsage
import sdl3.SDL_GPU_TEXTUREUSAGE_SAMPLER
import sdl3.SDL_GetError
import sdl3.SDL_IOFromConstMem
import sdl3.SDL_MapGPUTransferBuffer
import sdl3.SDL_ReleaseGPUSampler
import sdl3.SDL_ReleaseGPUTexture
import sdl3.SDL_ReleaseGPUTransferBuffer
import sdl3.SDL_SubmitGPUCommandBuffer
import sdl3.SDL_UnmapGPUTransferBuffer
import sdl3.SDL_UploadToGPUTexture
import sdl3.image.IMG_Load
import sdl3.image.IMG_Load_IO
import sdl3.image.SDL_Surface
import sdl3.image.SDL_ConvertSurface
import sdl3.image.SDL_DestroySurface
import sdl3.image.SDL_PIXELFORMAT_RGBA32

@OptIn(ExperimentalForeignApi::class)
class GpuTexture private constructor(
    private val gpu: GpuContext,
    val texture: CPointer<SDL_GPUTexture>,
    val sampler: CPointer<SDL_GPUSampler>,
    val width: UInt,
    val height: UInt
) {
    private var cleanedUp = false

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        SDL_ReleaseGPUSampler(gpu.device, sampler)
        SDL_ReleaseGPUTexture(gpu.device, texture)
    }

    companion object {
        fun fromFile(
            gpu: GpuContext,
            assetPath: String
        ): GpuTexture {
            val resolvedPath = File.resolveAssetPath(assetPath)
            val surface = IMG_Load(resolvedPath)
                ?: throw IllegalStateException("Error loading image for GPU texture: ${SDL_GetError()?.toKString()}")

            return fromSurface(gpu, surface, assetPath)
        }

        fun fromEncodedBytes(
            gpu: GpuContext,
            bytes: ByteArray,
            label: String = "embedded texture"
        ): GpuTexture {
            require(bytes.isNotEmpty()) {
                "Encoded GPU texture bytes must not be empty."
            }

            return bytes.usePinned { pinned ->
                val io = SDL_IOFromConstMem(pinned.addressOf(0), bytes.size.convert())
                    ?: throw IllegalStateException("Error opening image bytes for GPU texture: ${SDL_GetError()?.toKString()}")
                val surface = IMG_Load_IO(io, true)
                    ?: throw IllegalStateException("Error loading image bytes for GPU texture: ${SDL_GetError()?.toKString()}")

                fromSurface(gpu, surface, label)
            }
        }

        private fun fromSurface(
            gpu: GpuContext,
            surface: CPointer<SDL_Surface>,
            label: String
        ): GpuTexture {
            val converted = try {
                SDL_ConvertSurface(surface, SDL_PIXELFORMAT_RGBA32)
                    ?: throw IllegalStateException("Error converting image to RGBA32: ${SDL_GetError()?.toKString()}")
            } finally {
                SDL_DestroySurface(surface)
            }

            try {
                val width = converted.pointed.w.toUInt()
                val height = converted.pointed.h.toUInt()
                val pitch = converted.pointed.pitch
                val pixels = converted.pointed.pixels
                    ?: throw IllegalStateException("Converted image has no pixel data: $label")
                val source = pixels.reinterpret<ByteVar>()
                val bytes = ByteArray((width * height * 4u).toInt())

                for (y in 0 until height.toInt()) {
                    val sourceRow = y * pitch
                    val destinationRow = y * width.toInt() * 4
                    for (x in 0 until width.toInt() * 4) {
                        bytes[destinationRow + x] = source[sourceRow + x]
                    }
                }

                return createRgba8(gpu, width, height, bytes)
            } finally {
                SDL_DestroySurface(converted)
            }
        }

        fun createRgba8(
            gpu: GpuContext,
            width: UInt,
            height: UInt,
            pixels: ByteArray
        ): GpuTexture {
            require(width > 0u && height > 0u) {
                "GpuTexture dimensions must be greater than zero."
            }
            require(pixels.size == (width * height * 4u).toInt()) {
                "GpuTexture RGBA8 pixel data must be width * height * 4 bytes."
            }

            val texture = createTexture(gpu, width, height)
            val sampler = try {
                createSampler(gpu)
            } catch (e: Throwable) {
                SDL_ReleaseGPUTexture(gpu.device, texture)
                throw e
            }

            try {
                upload(gpu, texture, width, height, pixels)
            } catch (e: Throwable) {
                SDL_ReleaseGPUSampler(gpu.device, sampler)
                SDL_ReleaseGPUTexture(gpu.device, texture)
                throw e
            }

            return GpuTexture(gpu, texture, sampler, width, height)
        }

        fun checkerboard(
            gpu: GpuContext,
            width: UInt = 128u,
            height: UInt = 128u,
            cells: UInt = 8u
        ): GpuTexture {
            val bytes = ByteArray((width * height * 4u).toInt())
            val cellWidth = (width / cells).coerceAtLeast(1u)
            val cellHeight = (height / cells).coerceAtLeast(1u)

            for (y in 0u until height) {
                for (x in 0u until width) {
                    val bright = ((x / cellWidth) + (y / cellHeight)) % 2u == 0u
                    val base = ((y * width + x) * 4u).toInt()
                    if (bright) {
                        bytes[base] = 240.toByte()
                        bytes[base + 1] = 230.toByte()
                        bytes[base + 2] = 196.toByte()
                    } else {
                        bytes[base] = 24.toByte()
                        bytes[base + 1] = 70.toByte()
                        bytes[base + 2] = 116.toByte()
                    }
                    bytes[base + 3] = 255.toByte()
                }
            }

            return createRgba8(gpu, width, height, bytes)
        }

        private fun createTexture(
            gpu: GpuContext,
            width: UInt,
            height: UInt
        ): CPointer<SDL_GPUTexture> {
            return memScoped {
                val createInfo = alloc<SDL_GPUTextureCreateInfo>()
                createInfo.type = SDL_GPUTextureType.SDL_GPU_TEXTURETYPE_2D
                createInfo.format = SDL_GPUTextureFormat.SDL_GPU_TEXTUREFORMAT_R8G8B8A8_UNORM
                createInfo.usage = SDL_GPU_TEXTUREUSAGE_SAMPLER
                createInfo.width = width
                createInfo.height = height
                createInfo.layer_count_or_depth = 1u
                createInfo.num_levels = 1u
                createInfo.sample_count = SDL_GPUSampleCount.SDL_GPU_SAMPLECOUNT_1
                createInfo.props = 0u

                SDL_CreateGPUTexture(gpu.device, createInfo.ptr)
                    ?: throw IllegalStateException("Error creating GPU texture: ${SDL_GetError()?.toKString()}")
            }
        }

        private fun createSampler(gpu: GpuContext): CPointer<SDL_GPUSampler> {
            return memScoped {
                val createInfo = alloc<SDL_GPUSamplerCreateInfo>()
                createInfo.min_filter = SDL_GPUFilter.SDL_GPU_FILTER_NEAREST
                createInfo.mag_filter = SDL_GPUFilter.SDL_GPU_FILTER_NEAREST
                createInfo.mipmap_mode = SDL_GPUSamplerMipmapMode.SDL_GPU_SAMPLERMIPMAPMODE_NEAREST
                createInfo.address_mode_u = SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_REPEAT
                createInfo.address_mode_v = SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_REPEAT
                createInfo.address_mode_w = SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_REPEAT
                createInfo.mip_lod_bias = 0f
                createInfo.max_anisotropy = 1f
                createInfo.compare_op = SDL_GPUCompareOp.SDL_GPU_COMPAREOP_INVALID
                createInfo.min_lod = 0f
                createInfo.max_lod = 0f
                createInfo.enable_anisotropy = false
                createInfo.enable_compare = false
                createInfo.props = 0u

                SDL_CreateGPUSampler(gpu.device, createInfo.ptr)
                    ?: throw IllegalStateException("Error creating GPU sampler: ${SDL_GetError()?.toKString()}")
            }
        }

        private fun upload(
            gpu: GpuContext,
            texture: CPointer<SDL_GPUTexture>,
            width: UInt,
            height: UInt,
            pixels: ByteArray
        ) {
            val byteSize = pixels.size.toUInt()
            val transferBuffer = createTransferBuffer(gpu, byteSize)

            try {
                val mapped = SDL_MapGPUTransferBuffer(gpu.device, transferBuffer, false)
                    ?: throw IllegalStateException("Error mapping GPU texture transfer buffer: ${SDL_GetError()?.toKString()}")

                pixels.usePinned { pinned ->
                    memcpy(
                        mapped,
                        pinned.addressOf(0),
                        byteSize.convert()
                    )
                }
                SDL_UnmapGPUTransferBuffer(gpu.device, transferBuffer)

                memScoped {
                    val commandBuffer = SDL_AcquireGPUCommandBuffer(gpu.device)
                        ?: throw IllegalStateException("Error acquiring GPU texture command buffer: ${SDL_GetError()?.toKString()}")
                    val copyPass = SDL_BeginGPUCopyPass(commandBuffer)
                        ?: throw IllegalStateException("Error beginning GPU texture copy pass: ${SDL_GetError()?.toKString()}")

                    val source = alloc<SDL_GPUTextureTransferInfo>()
                    source.transfer_buffer = transferBuffer
                    source.offset = 0u
                    source.pixels_per_row = width
                    source.rows_per_layer = height

                    val destination = alloc<SDL_GPUTextureRegion>()
                    destination.texture = texture
                    destination.mip_level = 0u
                    destination.layer = 0u
                    destination.x = 0u
                    destination.y = 0u
                    destination.z = 0u
                    destination.w = width
                    destination.h = height
                    destination.d = 1u

                    SDL_UploadToGPUTexture(copyPass, source.ptr, destination.ptr, false)
                    SDL_EndGPUCopyPass(copyPass)

                    if (!SDL_SubmitGPUCommandBuffer(commandBuffer)) {
                        throw IllegalStateException("Error submitting GPU texture upload command buffer: ${SDL_GetError()?.toKString()}")
                    }
                }
            } finally {
                SDL_ReleaseGPUTransferBuffer(gpu.device, transferBuffer)
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
                    ?: throw IllegalStateException("Error creating GPU texture transfer buffer: ${SDL_GetError()?.toKString()}")
            }
        }
    }
}
