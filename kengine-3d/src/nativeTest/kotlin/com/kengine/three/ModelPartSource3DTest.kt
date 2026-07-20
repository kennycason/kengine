package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ModelPartSource3DTest {
    @Test
    fun parsedModelDefaultsToOneLitSourcePart() {
        val vertices = testLitTriangle()
        val parsed = ParsedModel3D(
            assetPath = "models/test.obj",
            format = ModelFormat3D.OBJ,
            options = ModelLoadOptions3D(),
            info = ModelInfo3D(
                assetPath = "models/test.obj",
                format = ModelFormat3D.OBJ,
                vertexCount = vertices.size
            ),
            litVertices = vertices
        )

        assertEquals(vertices, parsed.litVertices)
        assertEquals(1, parsed.parts.size)
        assertEquals(vertices.size, parsed.parts.single().vertexCount)
        assertIs<ModelPartSource3D.Lit>(parsed.parts.single())
    }

    @Test
    fun litSourceRejectsTexturedMaterialDescriptors() {
        assertFailsWith<IllegalArgumentException> {
            ModelPartSource3D.lit(
                vertices = testLitTriangle(),
                materialDescriptor = MaterialDescriptor3D.textured(
                    textureAsset = GpuTextureAsset3D.whiteRgba8("test:white")
                )
            )
        }
    }

    @Test
    fun texturedLitSourceRequiresTexturedMaterialDescriptor() {
        assertFailsWith<IllegalArgumentException> {
            ModelPartSource3D.texturedLit(
                vertices = testTexturedLitTriangle(),
                materialDescriptor = MaterialDescriptor3D.solid()
            )
        }
    }

    private fun testLitTriangle(): List<LitVertex3D> {
        return listOf(
            LitVertex3D(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff")),
            LitVertex3D(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff")),
            LitVertex3D(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff"))
        )
    }

    private fun testTexturedLitTriangle(): List<TexturedLitVertex3D> {
        return listOf(
            TexturedLitVertex3D(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff"), 0f, 0f),
            TexturedLitVertex3D(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff"), 1f, 0f),
            TexturedLitVertex3D(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 1.0, 0.0), Color.fromHex("ffffff"), 0f, 1f)
        )
    }
}
