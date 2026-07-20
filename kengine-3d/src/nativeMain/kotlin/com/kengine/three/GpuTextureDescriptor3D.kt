package com.kengine.three

import sdl3.SDL_GPUCompareOp
import sdl3.SDL_GPUFilter
import sdl3.SDL_GPUSampleCount
import sdl3.SDL_GPUSamplerAddressMode
import sdl3.SDL_GPUSamplerMipmapMode
import sdl3.SDL_GPUTextureFormat

enum class GpuTextureAddressMode {
    REPEAT,
    MIRRORED_REPEAT,
    CLAMP_TO_EDGE
}

enum class GpuTextureFilter3D(
    internal val sdlFilter: SDL_GPUFilter
) {
    NEAREST(SDL_GPUFilter.SDL_GPU_FILTER_NEAREST),
    LINEAR(SDL_GPUFilter.SDL_GPU_FILTER_LINEAR)
}

enum class GpuTextureMipmapMode3D(
    internal val sdlMipmapMode: SDL_GPUSamplerMipmapMode
) {
    NEAREST(SDL_GPUSamplerMipmapMode.SDL_GPU_SAMPLERMIPMAPMODE_NEAREST),
    LINEAR(SDL_GPUSamplerMipmapMode.SDL_GPU_SAMPLERMIPMAPMODE_LINEAR)
}

enum class GpuTextureFormat3D(
    internal val sdlFormat: SDL_GPUTextureFormat,
    internal val bytesPerPixel: UInt
) {
    RGBA8_UNORM(SDL_GPUTextureFormat.SDL_GPU_TEXTUREFORMAT_R8G8B8A8_UNORM, 4u)
}

enum class GpuTextureSampleCount3D(
    internal val sdlSampleCount: SDL_GPUSampleCount
) {
    ONE(SDL_GPUSampleCount.SDL_GPU_SAMPLECOUNT_1)
}

data class GpuTextureDescriptor3D(
    val width: UInt,
    val height: UInt,
    val format: GpuTextureFormat3D = GpuTextureFormat3D.RGBA8_UNORM,
    val layerCountOrDepth: UInt = 1u,
    val mipLevels: UInt = 1u,
    val sampleCount: GpuTextureSampleCount3D = GpuTextureSampleCount3D.ONE
) {
    init {
        require(width > 0u && height > 0u) {
            "GpuTexture dimensions must be greater than zero."
        }
        require(layerCountOrDepth > 0u) {
            "GpuTexture layer count or depth must be greater than zero."
        }
        require(mipLevels > 0u) {
            "GpuTexture mip levels must be greater than zero."
        }
    }

    fun requiredByteSize(): Int {
        val byteSize = width.toULong() *
            height.toULong() *
            layerCountOrDepth.toULong() *
            format.bytesPerPixel.toULong()
        require(byteSize <= Int.MAX_VALUE.toULong()) {
            "GpuTexture pixel data is too large to upload."
        }
        return byteSize.toInt()
    }

    companion object {
        fun rgba8(
            width: UInt,
            height: UInt
        ): GpuTextureDescriptor3D {
            return GpuTextureDescriptor3D(
                width = width,
                height = height
            )
        }
    }
}

data class GpuSamplerDescriptor3D(
    val minFilter: GpuTextureFilter3D = GpuTextureFilter3D.NEAREST,
    val magFilter: GpuTextureFilter3D = GpuTextureFilter3D.NEAREST,
    val mipmapMode: GpuTextureMipmapMode3D = GpuTextureMipmapMode3D.NEAREST,
    val addressModeU: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT,
    val addressModeV: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT,
    val addressModeW: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT,
    val mipLodBias: Float = 0f,
    val maxAnisotropy: Float = 1f,
    val compareOp: SDL_GPUCompareOp = SDL_GPUCompareOp.SDL_GPU_COMPAREOP_INVALID,
    val minLod: Float = 0f,
    val maxLod: Float = 0f,
    val enableAnisotropy: Boolean = false,
    val enableCompare: Boolean = false
) {
    init {
        require(maxAnisotropy >= 1f) {
            "GpuSampler max anisotropy must be at least 1."
        }
        require(maxLod >= minLod) {
            "GpuSampler max LOD must be greater than or equal to min LOD."
        }
    }

    companion object {
        val NEAREST_REPEAT = GpuSamplerDescriptor3D()

        fun nearest(
            addressModeU: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT,
            addressModeV: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT,
            addressModeW: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT
        ): GpuSamplerDescriptor3D {
            return GpuSamplerDescriptor3D(
                addressModeU = addressModeU,
                addressModeV = addressModeV,
                addressModeW = addressModeW
            )
        }

        fun linear(
            addressModeU: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT,
            addressModeV: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT,
            addressModeW: GpuTextureAddressMode = GpuTextureAddressMode.REPEAT
        ): GpuSamplerDescriptor3D {
            return GpuSamplerDescriptor3D(
                minFilter = GpuTextureFilter3D.LINEAR,
                magFilter = GpuTextureFilter3D.LINEAR,
                mipmapMode = GpuTextureMipmapMode3D.LINEAR,
                addressModeU = addressModeU,
                addressModeV = addressModeV,
                addressModeW = addressModeW
            )
        }
    }
}

data class GpuTextureUploadDescriptor3D(
    val pixelsPerRow: UInt? = null,
    val rowsPerLayer: UInt? = null,
    val cycle: Boolean = false
) {
    init {
        pixelsPerRow?.let { value ->
            require(value > 0u) {
                "GpuTexture upload pixels per row must be greater than zero."
            }
        }
        rowsPerLayer?.let { value ->
            require(value > 0u) {
                "GpuTexture upload rows per layer must be greater than zero."
            }
        }
    }

    internal fun resolvedPixelsPerRow(texture: GpuTextureDescriptor3D): UInt {
        return pixelsPerRow ?: texture.width
    }

    internal fun resolvedRowsPerLayer(texture: GpuTextureDescriptor3D): UInt {
        return rowsPerLayer ?: texture.height
    }

    internal fun requiredByteSize(texture: GpuTextureDescriptor3D): Int {
        val resolvedPixelsPerRow = resolvedPixelsPerRow(texture)
        val resolvedRowsPerLayer = resolvedRowsPerLayer(texture)
        require(resolvedPixelsPerRow >= texture.width) {
            "GpuTexture upload pixels per row must be greater than or equal to texture width."
        }
        require(resolvedRowsPerLayer >= texture.height) {
            "GpuTexture upload rows per layer must be greater than or equal to texture height."
        }

        val byteSize = resolvedPixelsPerRow.toULong() *
            resolvedRowsPerLayer.toULong() *
            texture.layerCountOrDepth.toULong() *
            texture.format.bytesPerPixel.toULong()
        require(byteSize <= Int.MAX_VALUE.toULong()) {
            "GpuTexture pixel data is too large to upload."
        }
        return byteSize.toInt()
    }
}

internal fun GpuTextureAddressMode.toSdl(): SDL_GPUSamplerAddressMode {
    return when (this) {
        GpuTextureAddressMode.REPEAT -> SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_REPEAT
        GpuTextureAddressMode.MIRRORED_REPEAT -> SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_MIRRORED_REPEAT
        GpuTextureAddressMode.CLAMP_TO_EDGE -> SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_CLAMP_TO_EDGE
    }
}
