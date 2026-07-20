package com.kengine.three

import com.kengine.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MaterialDescriptor3DTest {
    @Test
    fun solidMaterialDescriptorCarriesNameAndColor() {
        val color = Color.fromHex("336699")
        val descriptor = MaterialDescriptor3D.solid(
            color = color,
            name = "paint"
        )

        assertEquals("paint", descriptor.name)
        assertEquals(color, descriptor.baseColor)
        assertFalse(descriptor.hasTexture)
        assertFalse(descriptor.hasSecondaryTextures)
        assertEquals(0, descriptor.textureCount)
        assertEquals(null, descriptor.textureAsset)
    }

    @Test
    fun texturedMaterialDescriptorCarriesTextureAsset() {
        val textureAsset = GpuTextureAsset3D.whiteRgba8("material:white")
        val descriptor = MaterialDescriptor3D.textured(
            textureAsset = textureAsset,
            color = Color.fromHex("ffffff"),
            name = "white"
        )

        assertEquals("white", descriptor.name)
        assertTrue(descriptor.hasTexture)
        assertFalse(descriptor.hasSecondaryTextures)
        assertEquals(1, descriptor.textureCount)
        assertSame(textureAsset, descriptor.textureAsset)
        assertSame(textureAsset, descriptor.textures.baseColor)
    }

    @Test
    fun descriptorCarriesSecondaryTextureSlotsWithoutChangingRenderableTexture() {
        val normal = GpuTextureAsset3D.whiteRgba8("material:normal")
        val specular = GpuTextureAsset3D.whiteRgba8("material:specular")
        val descriptor = MaterialDescriptor3D.solid(
            textures = MaterialTextureSet3D(
                normal = normal,
                specular = specular
            )
        )

        assertFalse(descriptor.hasTexture)
        assertTrue(descriptor.hasSecondaryTextures)
        assertEquals(2, descriptor.textureCount)
        assertEquals(null, descriptor.textureAsset)
        assertSame(normal, descriptor.textures.normal)
        assertSame(specular, descriptor.textures.specular)
    }
}
