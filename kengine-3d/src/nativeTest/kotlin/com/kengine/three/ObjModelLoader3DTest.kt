package com.kengine.three

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getpid
import platform.posix.system
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ObjModelLoader3DTest {
    @Test
    fun loadsObjMtlDiffuseTextureAsTexturedModelPart() {
        val fixture = ObjFixture.writeTexturedAndSolid()

        val source = ModelLoader3D.loadSource(
            assetPath = fixture.objPath,
            options = ModelLoadOptions3D(normalize = false)
        )

        assertEquals(ModelFormat3D.OBJ, source.format)
        assertEquals(ModelFormat3D.OBJ, source.info.format)
        assertEquals(6, source.info.vertexCount)
        assertEquals(2, source.info.primitiveCount)
        assertEquals(2, source.info.materialCount)
        assertEquals(1, source.info.textureCount)
        assertEquals(1, source.info.imageCount)
        assertEquals(1, source.info.textureSlotUsage.baseColor)
        assertEquals(0, source.info.textureSlotUsage.normal)
        assertTrue(source.info.hasTexturedMaterials)
        assertEquals(2, source.parts.size)

        val texturedPart = assertIs<ModelPartSource3D.TexturedLit>(source.parts[0])
        assertEquals(3, texturedPart.vertexCount)
        assertEquals("textured", texturedPart.materialDescriptor.name)
        assertTrue(texturedPart.materialDescriptor.hasTexture)
        assertEquals(
            "file:${fixture.texturePath}",
            texturedPart.materialDescriptor.textureAsset?.key?.id
        )
        assertEquals(0f, texturedPart.vertices[0].u)
        assertEquals(1f, texturedPart.vertices[0].v)

        val solidPart = assertIs<ModelPartSource3D.Lit>(source.parts[1])
        assertEquals(3, solidPart.vertexCount)
        assertEquals("paint", solidPart.materialDescriptor.name)
        assertFalse(solidPart.materialDescriptor.hasTexture)
    }

    @Test
    fun loadsAdditionalMtlTextureMapsAsMaterialMetadata() {
        val fixture = ObjFixture.writeTexturedAndSolid(includeSecondaryTextureMaps = true)

        val source = ModelLoader3D.loadSource(
            assetPath = fixture.objPath,
            options = ModelLoadOptions3D(normalize = false)
        )

        assertEquals(9, source.info.textureCount)
        assertEquals(9, source.info.imageCount)
        assertEquals(1, source.info.textureSlotUsage.baseColor)
        assertEquals(1, source.info.textureSlotUsage.normal)
        assertEquals(1, source.info.textureSlotUsage.specular)
        assertEquals(1, source.info.textureSlotUsage.roughness)
        assertEquals(1, source.info.textureSlotUsage.metallic)
        assertEquals(1, source.info.textureSlotUsage.emissive)
        assertEquals(1, source.info.textureSlotUsage.ambient)
        assertEquals(1, source.info.textureSlotUsage.alpha)
        assertEquals(1, source.info.textureSlotUsage.displacement)
        assertTrue(source.info.hasTexturedMaterials)

        val texturedPart = assertIs<ModelPartSource3D.TexturedLit>(source.parts[0])
        val descriptor = texturedPart.materialDescriptor
        assertTrue(descriptor.hasTexture)
        assertTrue(descriptor.hasSecondaryTextures)
        assertEquals(9, descriptor.textureCount)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("normal")}", descriptor.textures.normal?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("specular")}", descriptor.textures.specular?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("roughness")}", descriptor.textures.roughness?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("metallic")}", descriptor.textures.metallic?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("emissive")}", descriptor.textures.emissive?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("ambient")}", descriptor.textures.ambient?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("alpha")}", descriptor.textures.alpha?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("displacement")}", descriptor.textures.displacement?.key?.id)
    }

    @Test
    fun preservesAbsoluteMtlDiffuseTexturePath() {
        val fixture = ObjFixture.writeTexturedAndSolid(useAbsoluteTexturePath = true)

        val source = ModelLoader3D.loadSource(
            assetPath = fixture.objPath,
            options = ModelLoadOptions3D(normalize = false)
        )

        val texturedPart = assertIs<ModelPartSource3D.TexturedLit>(source.parts[0])
        assertEquals(
            "file:${fixture.texturePath}",
            texturedPart.materialDescriptor.textureAsset?.key?.id
        )
    }

    @Test
    fun resolvesRootRelativeMtlDiffuseTexturePathFromMaterialDirectory() {
        val fixture = ObjFixture.writeTexturedAndSolid(useRootRelativeTexturePath = true)

        val source = ModelLoader3D.loadSource(
            assetPath = fixture.objPath,
            options = ModelLoadOptions3D(normalize = false)
        )

        val texturedPart = assertIs<ModelPartSource3D.TexturedLit>(source.parts[0])
        assertEquals(
            "file:${fixture.texturePath}",
            texturedPart.materialDescriptor.textureAsset?.key?.id
        )
    }

    @Test
    fun missingObjMaterialLibraryReportsReferencedPath() {
        val fixture = ObjFixture.writeTexturedAndSolid(writeMaterialLibrary = false)

        val error = assertFailsWith<IllegalArgumentException> {
            ModelLoader3D.loadSource(
                assetPath = fixture.objPath,
                options = ModelLoadOptions3D(normalize = false)
            )
        }
        val message = error.message.orEmpty()

        assertTrue(message.contains("OBJ material library was not found: ${fixture.mtlPath}"))
        assertTrue(message.contains("referenced by ${fixture.objPath}"))
    }
}

private data class ObjFixture(
    val objPath: String,
    val mtlPath: String,
    val texturePath: String,
    val secondaryTexturePaths: Map<String, String> = emptyMap()
) {
    companion object {
        fun writeTexturedAndSolid(
            useAbsoluteTexturePath: Boolean = false,
            useRootRelativeTexturePath: Boolean = false,
            includeSecondaryTextureMaps: Boolean = false,
            writeMaterialLibrary: Boolean = true
        ): ObjFixture {
            require(!(useAbsoluteTexturePath && useRootRelativeTexturePath)) {
                "Texture path fixture can be absolute or root-relative, not both."
            }
            val baseSuffix = when {
                useAbsoluteTexturePath -> "absolute"
                useRootRelativeTexturePath -> "root-relative"
                else -> "relative"
            }
            val suffix = if (writeMaterialLibrary) baseSuffix else "$baseSuffix-missing-mtl"
            val dir = "/tmp/kengine-3d-obj-test-${getpid()}-$suffix"
            system("mkdir -p $dir")
            val textureDir = if (useRootRelativeTexturePath) "$dir/Maps" else dir
            system("mkdir -p $textureDir")
            val objPath = "$dir/mixed.obj"
            val mtlPath = "$dir/mixed.mtl"
            val texturePath = "$textureDir/texture albedo.png"
            val textureReference = when {
                useAbsoluteTexturePath -> texturePath
                useRootRelativeTexturePath -> "/Maps/texture albedo.png"
                else -> "texture albedo.png"
            }
            val secondaryTexturePaths = if (includeSecondaryTextureMaps) {
                mapOf(
                    "normal" to "$textureDir/normal map.png",
                    "specular" to "$textureDir/specular map.png",
                    "roughness" to "$textureDir/roughness map.png",
                    "metallic" to "$textureDir/metallic map.png",
                    "emissive" to "$textureDir/emissive map.png",
                    "ambient" to "$textureDir/ambient map.png",
                    "alpha" to "$textureDir/alpha map.png",
                    "displacement" to "$textureDir/displacement map.png"
                )
            } else {
                emptyMap()
            }
            val secondaryTextureLines = if (includeSecondaryTextureMaps) {
                """
                    map_Bump -bm 0.5 normal map.png
                    map_Ks specular map.png
                    map_Pr roughness map.png
                    map_Pm metallic map.png
                    map_Ke emissive map.png
                    map_Ka ambient map.png
                    map_d alpha map.png
                    disp displacement map.png
                """.trimIndent()
            } else {
                ""
            }

            writeText(
                path = objPath,
                text = """
                    mtllib mixed.mtl
                    v 0 0 0
                    v 1 0 0
                    v 0 1 0
                    v 0 0 1
                    vt 0 0
                    vt 1 0
                    vt 0 1
                    usemtl textured
                    f 1/1 2/2 3/3
                    usemtl paint
                    f 1 3 4
                """.trimIndent()
            )
            writeText(
                path = texturePath,
                text = "placeholder"
            )
            secondaryTexturePaths.values.forEach { path ->
                writeText(
                    path = path,
                    text = "placeholder"
                )
            }
            if (writeMaterialLibrary) {
                writeText(
                    path = mtlPath,
                    text = """
                        newmtl textured
                        Kd 0.5 0.75 1.0
                        map_Kd $textureReference
                        $secondaryTextureLines

                        newmtl paint
                        Kd 0.25 0.5 0.75
                    """.trimIndent()
                )
            }

            return ObjFixture(
                objPath = objPath,
                mtlPath = mtlPath,
                texturePath = texturePath,
                secondaryTexturePaths = secondaryTexturePaths
            )
        }

        @OptIn(ExperimentalForeignApi::class)
        private fun writeText(
            path: String,
            text: String
        ) {
            val bytes = text.encodeToByteArray()
            val file = fopen(path, "wb")
                ?: throw IllegalStateException("Could not open fixture file for writing: $path")
            try {
                val written = fwrite(bytes.refTo(0), 1.toULong(), bytes.size.toULong(), file)
                check(written == bytes.size.toULong()) {
                    "Could not write complete fixture file: $path"
                }
            } finally {
                fclose(file)
            }
        }
    }
}
