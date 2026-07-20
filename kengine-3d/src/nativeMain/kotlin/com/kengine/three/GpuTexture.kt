package com.kengine.three

import cnames.structs.SDL_GPUSampler
import cnames.structs.SDL_GPUTexture
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
import sdl3.SDL_CreateGPUSampler
import sdl3.SDL_CreateGPUTexture
import sdl3.SDL_GPUSamplerCreateInfo
import sdl3.SDL_GPUTextureCreateInfo
import sdl3.SDL_GPUTextureRegion
import sdl3.SDL_GPUTextureTransferInfo
import sdl3.SDL_GPUTextureType
import sdl3.SDL_GPU_TEXTUREUSAGE_SAMPLER
import sdl3.SDL_GetError
import sdl3.SDL_IOFromConstMem
import sdl3.SDL_ReleaseGPUSampler
import sdl3.SDL_ReleaseGPUTexture
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
    val descriptor: GpuTextureDescriptor3D,
    val samplerDescriptor: GpuSamplerDescriptor3D
) : GpuResource3D {
    val width: UInt
        get() = descriptor.width

    val height: UInt
        get() = descriptor.height

    private var cleanedUp = false

    override fun cleanup() {
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
            return fromFile(
                gpu = gpu,
                assetPath = assetPath,
                samplerDescriptor = GpuSamplerDescriptor3D.NEAREST_REPEAT
            )
        }

        fun fromFile(
            gpu: GpuContext,
            assetPath: String,
            samplerDescriptor: GpuSamplerDescriptor3D
        ): GpuTexture {
            val resolvedPath = File.resolveAssetPath(assetPath)
            val surface = IMG_Load(resolvedPath)
                ?: throw IllegalStateException("Error loading image for GPU texture: ${SDL_GetError()?.toKString()}")

            return fromSurface(gpu, surface, assetPath, samplerDescriptor)
        }

        fun fromFile(
            gpu: GpuContext,
            assetPath: String,
            addressModeU: GpuTextureAddressMode,
            addressModeV: GpuTextureAddressMode
        ): GpuTexture {
            return fromFile(
                gpu = gpu,
                assetPath = assetPath,
                samplerDescriptor = GpuSamplerDescriptor3D.nearest(
                    addressModeU = addressModeU,
                    addressModeV = addressModeV
                )
            )
        }

        fun fromEncodedBytes(
            gpu: GpuContext,
            bytes: ByteArray,
            label: String = "embedded texture"
        ): GpuTexture {
            return fromEncodedBytes(
                gpu = gpu,
                bytes = bytes,
                label = label,
                samplerDescriptor = GpuSamplerDescriptor3D.NEAREST_REPEAT
            )
        }

        fun fromEncodedBytes(
            gpu: GpuContext,
            bytes: ByteArray,
            label: String = "embedded texture",
            samplerDescriptor: GpuSamplerDescriptor3D
        ): GpuTexture {
            require(bytes.isNotEmpty()) {
                "Encoded GPU texture bytes must not be empty."
            }

            return bytes.usePinned { pinned ->
                val io = SDL_IOFromConstMem(pinned.addressOf(0), bytes.size.convert())
                    ?: throw IllegalStateException("Error opening image bytes for GPU texture: ${SDL_GetError()?.toKString()}")
                val surface = IMG_Load_IO(io, true)
                    ?: throw IllegalStateException("Error loading image bytes for GPU texture: ${SDL_GetError()?.toKString()}")

                fromSurface(gpu, surface, label, samplerDescriptor)
            }
        }

        fun fromEncodedBytes(
            gpu: GpuContext,
            bytes: ByteArray,
            label: String,
            addressModeU: GpuTextureAddressMode,
            addressModeV: GpuTextureAddressMode
        ): GpuTexture {
            return fromEncodedBytes(
                gpu = gpu,
                bytes = bytes,
                label = label,
                samplerDescriptor = GpuSamplerDescriptor3D.nearest(
                    addressModeU = addressModeU,
                    addressModeV = addressModeV
                )
            )
        }

        private fun fromSurface(
            gpu: GpuContext,
            surface: CPointer<SDL_Surface>,
            label: String,
            samplerDescriptor: GpuSamplerDescriptor3D
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

                return createRgba8(
                    gpu = gpu,
                    width = width,
                    height = height,
                    pixels = bytes,
                    samplerDescriptor = samplerDescriptor
                )
            } finally {
                SDL_DestroySurface(converted)
            }
        }

        fun createRgba8(
            gpu: GpuContext,
            width: UInt,
            height: UInt,
            pixels: ByteArray,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT,
            uploadDescriptor: GpuTextureUploadDescriptor3D = GpuTextureUploadDescriptor3D()
        ): GpuTexture {
            return createRgba8(
                gpu = gpu,
                descriptor = GpuTextureDescriptor3D.rgba8(width, height),
                pixels = pixels,
                samplerDescriptor = samplerDescriptor,
                uploadDescriptor = uploadDescriptor
            )
        }

        fun createRgba8(
            gpu: GpuContext,
            width: UInt,
            height: UInt,
            pixels: ByteArray,
            addressModeU: GpuTextureAddressMode,
            addressModeV: GpuTextureAddressMode
        ): GpuTexture {
            return createRgba8(
                gpu = gpu,
                width = width,
                height = height,
                pixels = pixels,
                samplerDescriptor = GpuSamplerDescriptor3D.nearest(
                    addressModeU = addressModeU,
                    addressModeV = addressModeV
                )
            )
        }

        fun createRgba8(
            gpu: GpuContext,
            descriptor: GpuTextureDescriptor3D,
            pixels: ByteArray,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT,
            uploadDescriptor: GpuTextureUploadDescriptor3D = GpuTextureUploadDescriptor3D()
        ): GpuTexture {
            require(descriptor.format == GpuTextureFormat3D.RGBA8_UNORM) {
                "GpuTexture.createRgba8 requires an RGBA8_UNORM descriptor."
            }
            require(pixels.size == uploadDescriptor.requiredByteSize(descriptor)) {
                "GpuTexture RGBA8 pixel data must match the texture upload layout."
            }

            val texture = createTexture(gpu, descriptor)
            val sampler = try {
                createSampler(gpu, samplerDescriptor)
            } catch (e: Throwable) {
                SDL_ReleaseGPUTexture(gpu.device, texture)
                throw e
            }

            try {
                upload(gpu, texture, descriptor, pixels, uploadDescriptor)
            } catch (e: Throwable) {
                SDL_ReleaseGPUSampler(gpu.device, sampler)
                SDL_ReleaseGPUTexture(gpu.device, texture)
                throw e
            }

            return GpuTexture(gpu, texture, sampler, descriptor, samplerDescriptor)
        }

        fun checkerboard(
            gpu: GpuContext,
            width: UInt = 128u,
            height: UInt = 128u,
            cells: UInt = 8u
        ): GpuTexture {
            return gpu.loadTexture3D(
                GpuTextureAsset3D.checkerboard(
                    width = width,
                    height = height,
                    cells = cells
                )
            )
        }

        private fun createTexture(
            gpu: GpuContext,
            descriptor: GpuTextureDescriptor3D
        ): CPointer<SDL_GPUTexture> {
            return memScoped {
                val createInfo = alloc<SDL_GPUTextureCreateInfo>()
                createInfo.type = SDL_GPUTextureType.SDL_GPU_TEXTURETYPE_2D
                createInfo.format = descriptor.format.sdlFormat
                createInfo.usage = SDL_GPU_TEXTUREUSAGE_SAMPLER
                createInfo.width = descriptor.width
                createInfo.height = descriptor.height
                createInfo.layer_count_or_depth = descriptor.layerCountOrDepth
                createInfo.num_levels = descriptor.mipLevels
                createInfo.sample_count = descriptor.sampleCount.sdlSampleCount
                createInfo.props = 0u

                SDL_CreateGPUTexture(gpu.device, createInfo.ptr)
                    ?: throw IllegalStateException("Error creating GPU texture: ${SDL_GetError()?.toKString()}")
            }
        }

        private fun createSampler(
            gpu: GpuContext,
            descriptor: GpuSamplerDescriptor3D
        ): CPointer<SDL_GPUSampler> {
            return memScoped {
                val createInfo = alloc<SDL_GPUSamplerCreateInfo>()
                createInfo.min_filter = descriptor.minFilter.sdlFilter
                createInfo.mag_filter = descriptor.magFilter.sdlFilter
                createInfo.mipmap_mode = descriptor.mipmapMode.sdlMipmapMode
                createInfo.address_mode_u = descriptor.addressModeU.toSdl()
                createInfo.address_mode_v = descriptor.addressModeV.toSdl()
                createInfo.address_mode_w = descriptor.addressModeW.toSdl()
                createInfo.mip_lod_bias = descriptor.mipLodBias
                createInfo.max_anisotropy = descriptor.maxAnisotropy
                createInfo.compare_op = descriptor.compareOp
                createInfo.min_lod = descriptor.minLod
                createInfo.max_lod = descriptor.maxLod
                createInfo.enable_anisotropy = descriptor.enableAnisotropy
                createInfo.enable_compare = descriptor.enableCompare
                createInfo.props = 0u

                SDL_CreateGPUSampler(gpu.device, createInfo.ptr)
                    ?: throw IllegalStateException("Error creating GPU sampler: ${SDL_GetError()?.toKString()}")
            }
        }

        private fun upload(
            gpu: GpuContext,
            texture: CPointer<SDL_GPUTexture>,
            descriptor: GpuTextureDescriptor3D,
            pixels: ByteArray,
            uploadDescriptor: GpuTextureUploadDescriptor3D
        ) {
            val byteSize = pixels.gpuByteSize3D()
            gpu.withUploadTransferBuffer3D(byteSize, label = "GPU texture") { transferBuffer ->
                gpu.copyByteDataToTransferBuffer3D(transferBuffer, pixels, label = "GPU texture")
                gpu.withGpuCopyPass3D(label = "GPU texture") { copyPass ->
                    memScoped {
                        val source = alloc<SDL_GPUTextureTransferInfo>()
                        source.transfer_buffer = transferBuffer
                        source.offset = 0u
                        source.pixels_per_row = uploadDescriptor.resolvedPixelsPerRow(descriptor)
                        source.rows_per_layer = uploadDescriptor.resolvedRowsPerLayer(descriptor)

                        val destination = alloc<SDL_GPUTextureRegion>()
                        destination.texture = texture
                        destination.mip_level = 0u
                        destination.layer = 0u
                        destination.x = 0u
                        destination.y = 0u
                        destination.z = 0u
                        destination.w = descriptor.width
                        destination.h = descriptor.height
                        destination.d = descriptor.layerCountOrDepth

                        SDL_UploadToGPUTexture(copyPass, source.ptr, destination.ptr, uploadDescriptor.cycle)
                    }
                }
            }
        }
    }
}
