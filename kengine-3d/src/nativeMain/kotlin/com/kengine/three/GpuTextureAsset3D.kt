package com.kengine.three

data class GpuTextureAssetKey3D(
    val id: String,
    val samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT
) {
    init {
        require(id.isNotBlank()) {
            "GpuTextureAssetKey3D id must not be blank."
        }
    }

    companion object {
        fun file(
            assetPath: String,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT
        ): GpuTextureAssetKey3D {
            require(assetPath.isNotBlank()) {
                "GPU texture file asset path must not be blank."
            }
            return GpuTextureAssetKey3D(
                id = "file:$assetPath",
                samplerDescriptor = samplerDescriptor
            )
        }

        fun encoded(
            cacheKey: String,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT
        ): GpuTextureAssetKey3D {
            require(cacheKey.isNotBlank()) {
                "GPU encoded texture cache key must not be blank."
            }
            return GpuTextureAssetKey3D(
                id = "encoded:$cacheKey",
                samplerDescriptor = samplerDescriptor
            )
        }

        fun rgba8(
            cacheKey: String,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT
        ): GpuTextureAssetKey3D {
            require(cacheKey.isNotBlank()) {
                "GPU RGBA8 texture cache key must not be blank."
            }
            return GpuTextureAssetKey3D(
                id = "rgba8:$cacheKey",
                samplerDescriptor = samplerDescriptor
            )
        }
    }
}

sealed class GpuTextureAsset3D {
    abstract val key: GpuTextureAssetKey3D
    internal abstract fun load(gpu: GpuContext): GpuTexture

    companion object {
        fun file(
            assetPath: String,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT
        ): GpuTextureAsset3D {
            require(assetPath.isNotBlank()) {
                "GPU texture file asset path must not be blank."
            }
            return FileGpuTextureAsset3D(assetPath, samplerDescriptor)
        }

        fun encodedBytes(
            cacheKey: String,
            bytes: ByteArray,
            label: String = cacheKey,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT
        ): GpuTextureAsset3D {
            require(cacheKey.isNotBlank()) {
                "GPU encoded texture cache key must not be blank."
            }
            require(bytes.isNotEmpty()) {
                "Encoded GPU texture bytes must not be empty."
            }
            return EncodedGpuTextureAsset3D(
                cacheKey = cacheKey,
                bytes = bytes.copyOf(),
                label = label,
                samplerDescriptor = samplerDescriptor
            )
        }

        internal fun encodedByteRange(
            cacheKey: String,
            bytes: ByteArray,
            byteOffset: Int,
            byteLength: Int,
            label: String = cacheKey,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT
        ): GpuTextureAsset3D {
            require(cacheKey.isNotBlank()) {
                "GPU encoded texture cache key must not be blank."
            }
            require(byteOffset >= 0) {
                "Encoded GPU texture byte offset must not be negative."
            }
            require(byteLength > 0) {
                "Encoded GPU texture byte length must be greater than zero."
            }
            require(byteOffset + byteLength <= bytes.size) {
                "Encoded GPU texture byte range must fit within the source bytes."
            }
            return EncodedByteRangeGpuTextureAsset3D(
                cacheKey = cacheKey,
                bytes = bytes,
                byteOffset = byteOffset,
                byteLength = byteLength,
                label = label,
                samplerDescriptor = samplerDescriptor
            )
        }

        fun rgba8(
            cacheKey: String,
            descriptor: GpuTextureDescriptor3D,
            pixels: ByteArray,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT,
            uploadDescriptor: GpuTextureUploadDescriptor3D = GpuTextureUploadDescriptor3D()
        ): GpuTextureAsset3D {
            require(cacheKey.isNotBlank()) {
                "GPU RGBA8 texture cache key must not be blank."
            }
            return Rgba8GpuTextureAsset3D(
                cacheKey = cacheKey,
                descriptor = descriptor,
                pixels = pixels.copyOf(),
                samplerDescriptor = samplerDescriptor,
                uploadDescriptor = uploadDescriptor
            )
        }

        fun whiteRgba8(
            cacheKey: String = "procedural:white-rgba8"
        ): GpuTextureAsset3D {
            return rgba8(
                cacheKey = cacheKey,
                descriptor = GpuTextureDescriptor3D.rgba8(width = 1u, height = 1u),
                pixels = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
            )
        }

        fun flatNormalRgba8(
            cacheKey: String = "procedural:flat-normal-rgba8"
        ): GpuTextureAsset3D {
            return rgba8(
                cacheKey = cacheKey,
                descriptor = GpuTextureDescriptor3D.rgba8(width = 1u, height = 1u),
                pixels = byteArrayOf(128.toByte(), 128.toByte(), 255.toByte(), 255.toByte())
            )
        }

        fun checkerboard(
            cacheKey: String? = null,
            width: UInt = 128u,
            height: UInt = 128u,
            cells: UInt = 8u,
            samplerDescriptor: GpuSamplerDescriptor3D = GpuSamplerDescriptor3D.NEAREST_REPEAT
        ): GpuTextureAsset3D {
            return rgba8(
                cacheKey = cacheKey ?: "procedural:checkerboard-rgba8:${width}x$height:$cells",
                descriptor = GpuTextureDescriptor3D.rgba8(width = width, height = height),
                pixels = createCheckerboardRgba8Pixels3D(width, height, cells),
                samplerDescriptor = samplerDescriptor
            )
        }
    }
}

fun GpuContext.loadTexture3D(asset: GpuTextureAsset3D): GpuTexture {
    return asset.load(this)
}

class GpuTextureCache3D(
    private val gpu: GpuContext
) : GpuResource3D {
    private val cache = GpuResourceCache3D<GpuTextureAssetKey3D, GpuTexture> { texture ->
        texture.cleanup()
    }

    val size: Int
        get() = cache.size

    fun load(asset: GpuTextureAsset3D): GpuTexture {
        return cache.getOrPut(asset.key) {
            asset.load(gpu)
        }
    }

    fun contains(asset: GpuTextureAsset3D): Boolean {
        return cache.containsKey(asset.key)
    }

    fun containsKey(key: GpuTextureAssetKey3D): Boolean {
        return cache.containsKey(key)
    }

    fun get(key: GpuTextureAssetKey3D): GpuTexture? {
        return cache.get(key)
    }

    override fun cleanup() {
        cache.cleanup()
    }
}

internal data class GpuTextureCacheScope3D(
    val cache: GpuTextureCache3D,
    val ownedResources: List<GpuResource3D>
)

internal fun GpuContext.resolveTextureCache3D(
    textureCache: GpuTextureCache3D?
): GpuTextureCacheScope3D {
    val cache = textureCache ?: GpuTextureCache3D(this)
    val ownedResources = if (textureCache == null) listOf(cache) else emptyList()
    return GpuTextureCacheScope3D(cache, ownedResources)
}

internal fun createCheckerboardRgba8Pixels3D(
    width: UInt,
    height: UInt,
    cells: UInt
): ByteArray {
    require(width > 0u && height > 0u) {
        "Checkerboard texture dimensions must be greater than zero."
    }
    require(cells > 0u) {
        "Checkerboard texture cell count must be greater than zero."
    }

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

    return bytes
}

private class FileGpuTextureAsset3D(
    private val assetPath: String,
    private val samplerDescriptor: GpuSamplerDescriptor3D
) : GpuTextureAsset3D() {
    override val key: GpuTextureAssetKey3D =
        GpuTextureAssetKey3D.file(
            assetPath = assetPath,
            samplerDescriptor = samplerDescriptor
        )

    override fun load(gpu: GpuContext): GpuTexture {
        return GpuTexture.fromFile(
            gpu = gpu,
            assetPath = assetPath,
            samplerDescriptor = samplerDescriptor
        )
    }
}

private class EncodedGpuTextureAsset3D(
    cacheKey: String,
    private val bytes: ByteArray,
    private val label: String,
    private val samplerDescriptor: GpuSamplerDescriptor3D
) : GpuTextureAsset3D() {
    override val key: GpuTextureAssetKey3D =
        GpuTextureAssetKey3D.encoded(
            cacheKey = cacheKey,
            samplerDescriptor = samplerDescriptor
        )

    override fun load(gpu: GpuContext): GpuTexture {
        return GpuTexture.fromEncodedBytes(
            gpu = gpu,
            bytes = bytes,
            label = label,
            samplerDescriptor = samplerDescriptor
        )
    }
}

private class EncodedByteRangeGpuTextureAsset3D(
    cacheKey: String,
    private val bytes: ByteArray,
    private val byteOffset: Int,
    private val byteLength: Int,
    private val label: String,
    private val samplerDescriptor: GpuSamplerDescriptor3D
) : GpuTextureAsset3D() {
    override val key: GpuTextureAssetKey3D =
        GpuTextureAssetKey3D.encoded(
            cacheKey = cacheKey,
            samplerDescriptor = samplerDescriptor
        )

    override fun load(gpu: GpuContext): GpuTexture {
        return GpuTexture.fromEncodedBytes(
            gpu = gpu,
            bytes = bytes.copyOfRange(byteOffset, byteOffset + byteLength),
            label = label,
            samplerDescriptor = samplerDescriptor
        )
    }
}

private class Rgba8GpuTextureAsset3D(
    cacheKey: String,
    private val descriptor: GpuTextureDescriptor3D,
    private val pixels: ByteArray,
    private val samplerDescriptor: GpuSamplerDescriptor3D,
    private val uploadDescriptor: GpuTextureUploadDescriptor3D
) : GpuTextureAsset3D() {
    override val key: GpuTextureAssetKey3D =
        GpuTextureAssetKey3D.rgba8(
            cacheKey = cacheKey,
            samplerDescriptor = samplerDescriptor
        )

    override fun load(gpu: GpuContext): GpuTexture {
        return GpuTexture.createRgba8(
            gpu = gpu,
            descriptor = descriptor,
            pixels = pixels,
            samplerDescriptor = samplerDescriptor,
            uploadDescriptor = uploadDescriptor
        )
    }
}
