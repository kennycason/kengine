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
        assertSame(textureAsset, descriptor.textureAsset)
    }
}
