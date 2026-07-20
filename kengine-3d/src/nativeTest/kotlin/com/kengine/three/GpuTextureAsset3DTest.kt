package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class GpuTextureAsset3DTest {
    @Test
    fun textureAssetKeysIncludeSourceAndSamplerPolicy() {
        val repeat = GpuTextureAsset3D.file("assets/textures/brick.png")
        val clamp = GpuTextureAsset3D.file(
            assetPath = "assets/textures/brick.png",
            samplerDescriptor = GpuSamplerDescriptor3D.nearest(
                addressModeU = GpuTextureAddressMode.CLAMP_TO_EDGE,
                addressModeV = GpuTextureAddressMode.CLAMP_TO_EDGE
            )
        )

        assertEquals(
            GpuTextureAsset3D.file("assets/textures/brick.png").key,
            repeat.key
        )
        assertNotEquals(repeat.key, clamp.key)
    }

    @Test
    fun checkerboardAssetKeyDefaultsToItsGeneratedShape() {
        assertEquals(
            "rgba8:procedural:checkerboard-rgba8:64x32:4",
            GpuTextureAsset3D.checkerboard(width = 64u, height = 32u, cells = 4u).key.id
        )
    }

    @Test
    fun encodedByteRangeAssetUsesEncodedKeyAndSamplerPolicy() {
        val sampler = GpuSamplerDescriptor3D.nearest(
            addressModeU = GpuTextureAddressMode.CLAMP_TO_EDGE,
            addressModeV = GpuTextureAddressMode.CLAMP_TO_EDGE
        )
        val asset = GpuTextureAsset3D.encodedByteRange(
            cacheKey = "glb:texture:0",
            bytes = byteArrayOf(0, 1, 2, 3, 4, 5),
            byteOffset = 1,
            byteLength = 4,
            samplerDescriptor = sampler
        )

        assertEquals(
            GpuTextureAssetKey3D.encoded(
                cacheKey = "glb:texture:0",
                samplerDescriptor = sampler
            ),
            asset.key
        )
    }

    @Test
    fun checkerboardPixelsMatchExpectedAlternatingPattern() {
        val pixels = createCheckerboardRgba8Pixels3D(width = 2u, height = 2u, cells = 2u)

        assertContentEquals(
            byteArrayOf(
                240.toByte(), 230.toByte(), 196.toByte(), 255.toByte(),
                24.toByte(), 70.toByte(), 116.toByte(), 255.toByte(),
                24.toByte(), 70.toByte(), 116.toByte(), 255.toByte(),
                240.toByte(), 230.toByte(), 196.toByte(), 255.toByte()
            ),
            pixels
        )
    }

    @Test
    fun textureAssetsRejectBlankKeysAndPaths() {
        assertFailsWith<IllegalArgumentException> {
            GpuTextureAsset3D.file("")
        }
        assertFailsWith<IllegalArgumentException> {
            GpuTextureAsset3D.encodedBytes(cacheKey = "", bytes = byteArrayOf(1))
        }
        assertFailsWith<IllegalArgumentException> {
            GpuTextureAsset3D.rgba8(
                cacheKey = "",
                descriptor = GpuTextureDescriptor3D.rgba8(width = 1u, height = 1u),
                pixels = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GpuTextureAsset3D.encodedByteRange(
                cacheKey = "glb:texture:0",
                bytes = byteArrayOf(1, 2),
                byteOffset = 1,
                byteLength = 2
            )
        }
    }
}
