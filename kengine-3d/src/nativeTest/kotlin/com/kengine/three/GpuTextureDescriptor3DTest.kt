package com.kengine.three

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GpuTextureDescriptor3DTest {
    @Test
    fun textureDescriptorComputesTightlyPackedRgba8ByteSize() {
        assertEquals(
            32,
            GpuTextureDescriptor3D.rgba8(width = 4u, height = 2u).requiredByteSize()
        )
        assertEquals(
            64,
            GpuTextureDescriptor3D(width = 4u, height = 2u, layerCountOrDepth = 2u).requiredByteSize()
        )
    }

    @Test
    fun textureDescriptorRejectsEmptyDimensions() {
        assertFailsWith<IllegalArgumentException> {
            GpuTextureDescriptor3D.rgba8(width = 0u, height = 1u)
        }
        assertFailsWith<IllegalArgumentException> {
            GpuTextureDescriptor3D.rgba8(width = 1u, height = 0u)
        }
    }

    @Test
    fun samplerPresetsKeepCurrentPixelTextureDefaults() {
        val descriptor = GpuSamplerDescriptor3D.NEAREST_REPEAT

        assertEquals(GpuTextureFilter3D.NEAREST, descriptor.minFilter)
        assertEquals(GpuTextureFilter3D.NEAREST, descriptor.magFilter)
        assertEquals(GpuTextureMipmapMode3D.NEAREST, descriptor.mipmapMode)
        assertEquals(GpuTextureAddressMode.REPEAT, descriptor.addressModeU)
        assertEquals(GpuTextureAddressMode.REPEAT, descriptor.addressModeV)
        assertEquals(GpuTextureAddressMode.REPEAT, descriptor.addressModeW)
    }

    @Test
    fun uploadDescriptorComputesPaddedRgba8ByteSize() {
        val texture = GpuTextureDescriptor3D.rgba8(width = 4u, height = 2u)
        val upload = GpuTextureUploadDescriptor3D(
            pixelsPerRow = 8u,
            rowsPerLayer = 3u
        )

        assertEquals(96, upload.requiredByteSize(texture))
    }

    @Test
    fun uploadDescriptorRejectsLayoutsSmallerThanTexture() {
        val texture = GpuTextureDescriptor3D.rgba8(width = 4u, height = 2u)

        assertFailsWith<IllegalArgumentException> {
            GpuTextureUploadDescriptor3D(pixelsPerRow = 3u).requiredByteSize(texture)
        }
        assertFailsWith<IllegalArgumentException> {
            GpuTextureUploadDescriptor3D(rowsPerLayer = 1u).requiredByteSize(texture)
        }
    }
}
