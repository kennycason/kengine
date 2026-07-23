package com.kengine.three

import com.kengine.graphics.Color
import com.kengine.math.Vec3
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
import kotlin.test.assertTrue

class ModelLoader3DTest {
    @Test
    fun detectsSupportedFormatsCaseInsensitively() {
        assertEquals(ModelFormat3D.GLB, ModelLoader3D.detectFormat("assets/models/world.glb"))
        assertEquals(ModelFormat3D.GLB, ModelLoader3D.detectFormat("assets/models/WORLD.GLB"))
        assertEquals(ModelFormat3D.GLTF, ModelLoader3D.detectFormat("assets/models/world.gltf"))
        assertEquals(ModelFormat3D.GLTF, ModelLoader3D.detectFormat("assets/models/WORLD.GLTF"))
        assertEquals(ModelFormat3D.OBJ, ModelLoader3D.detectFormat("assets/models/ship.obj"))
        assertEquals(ModelFormat3D.OBJ, ModelLoader3D.detectFormat("assets/models/SHIP.OBJ"))
    }

    @Test
    fun rejectsUnsupportedFormats() {
        assertFailsWith<IllegalArgumentException> {
            ModelLoader3D.detectFormat("assets/models/world.fbx")
        }
    }

    @Test
    fun loadsGltfWithExternalBufferAndImageUri() {
        val fixture = GltfFixture.write()

        val source = ModelLoader3D.loadSource(
            assetPath = fixture.modelPath,
            options = ModelLoadOptions3D(normalize = false)
        )
        val info = ModelLoader3D.inspect(fixture.modelPath)

        assertEquals(ModelFormat3D.GLTF, source.format)
        assertEquals(ModelFormat3D.GLTF, source.info.format)
        assertEquals(3, source.info.vertexCount)
        assertEquals(1, source.info.meshCount)
        assertEquals(1, source.info.primitiveCount)
        assertEquals(1, source.info.materialCount)
        assertEquals(1, source.info.textureCount)
        assertEquals(1, source.info.imageCount)
        assertEquals(1, source.info.textureSlotUsage.baseColor)
        assertEquals(0, source.info.textureSlotUsage.normal)
        assertEquals(ModelFormat3D.GLTF, info.format)
        assertEquals(1, info.meshCount)
        assertEquals(1, info.materialCount)
        assertEquals(1, info.textureCount)
        assertEquals(1, info.imageCount)
        assertEquals(Vec3(0.0, 0.0, 0.0), source.litVertices[0].position)
        assertEquals(Vec3(1.0, 0.0, 0.0), source.litVertices[1].position)
        assertEquals(Vec3(0.0, 1.0, 0.0), source.litVertices[2].position)
        assertTrue(source.parts.single().materialDescriptor.hasTexture)
        assertEquals(
            "file:${fixture.texturePath}",
            source.parts.single().materialDescriptor.textureAsset?.key?.id
        )
    }

    @Test
    fun loadsGltfMaterialTextureSlots() {
        val fixture = GltfFixture.write(includeMaterialTextureSlots = true)

        val source = ModelLoader3D.loadSource(
            assetPath = fixture.modelPath,
            options = ModelLoadOptions3D(normalize = false)
        )

        assertEquals(6, source.info.textureCount)
        assertEquals(6, source.info.imageCount)
        assertEquals(1, source.info.textureSlotUsage.baseColor)
        assertEquals(1, source.info.textureSlotUsage.normal)
        assertEquals(1, source.info.textureSlotUsage.metallicRoughness)
        assertEquals(1, source.info.textureSlotUsage.emissive)
        assertEquals(1, source.info.textureSlotUsage.ambient)
        assertEquals(1, source.info.textureSlotUsage.specular)
        val descriptor = source.parts.single().materialDescriptor
        assertTrue(descriptor.hasTexture)
        assertTrue(descriptor.hasSecondaryTextures)
        assertEquals(6, descriptor.textureCount)
        assertEquals("file:${fixture.texturePath}", descriptor.textures.baseColor?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("normal")}", descriptor.textures.normal?.key?.id)
        assertEquals(
            "file:${fixture.secondaryTexturePaths.getValue("metallicRoughness")}",
            descriptor.textures.metallicRoughness?.key?.id
        )
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("emissive")}", descriptor.textures.emissive?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("occlusion")}", descriptor.textures.ambient?.key?.id)
        assertEquals("file:${fixture.secondaryTexturePaths.getValue("specular")}", descriptor.textures.specular?.key?.id)
    }

    @Test
    fun usesEmissiveTextureAsRenderableFallbackWhenBaseColorTextureIsMissing() {
        val fixture = GltfFixture.write(emissiveOnlyMaterial = true)

        val source = ModelLoader3D.loadSource(
            assetPath = fixture.modelPath,
            options = ModelLoadOptions3D(normalize = false)
        )

        assertTrue(source.info.hasTexturedMaterials)
        assertEquals(0, source.info.textureSlotUsage.baseColor)
        assertEquals(1, source.info.textureSlotUsage.emissive)
        val descriptor = source.parts.single().materialDescriptor
        assertTrue(descriptor.hasTexture)
        assertEquals(Color.fromRGBA(1.0f, 0.5f, 0.25f), descriptor.baseColor)
        assertEquals("file:${fixture.texturePath}", descriptor.textureAsset?.key?.id)
        assertEquals("file:${fixture.texturePath}", descriptor.textures.baseColor?.key?.id)
        assertEquals("file:${fixture.texturePath}", descriptor.textures.emissive?.key?.id)
    }

    @Test
    fun missingGltfExternalBufferReportsReferencedPath() {
        val fixture = GltfFixture.write(writeBuffer = false)

        val error = assertFailsWith<IllegalArgumentException> {
            ModelLoader3D.loadSource(
                assetPath = fixture.modelPath,
                options = ModelLoadOptions3D(normalize = false)
            )
        }
        val message = error.message.orEmpty()

        assertTrue(message.contains("GLTF buffer 'triangle%20mesh.bin' was not found: ${fixture.bufferPath}"))
        assertTrue(message.contains("referenced by ${fixture.modelPath}"))
    }

    @Test
    fun missingGltfExternalImageReportsReferencedPath() {
        val fixture = GltfFixture.write(writeImages = false)

        val error = assertFailsWith<IllegalArgumentException> {
            ModelLoader3D.loadSource(
                assetPath = fixture.modelPath,
                options = ModelLoadOptions3D(normalize = false)
            )
        }
        val message = error.message.orEmpty()

        assertTrue(message.contains("GLTF image 'texture%20albedo.png' was not found: ${fixture.texturePath}"))
        assertTrue(message.contains("referenced by ${fixture.modelPath}"))
    }
}

private data class GltfFixture(
    val modelPath: String,
    val bufferPath: String,
    val texturePath: String,
    val secondaryTexturePaths: Map<String, String> = emptyMap()
) {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun write(
            includeMaterialTextureSlots: Boolean = false,
            emissiveOnlyMaterial: Boolean = false,
            writeBuffer: Boolean = true,
            writeImages: Boolean = true
        ): GltfFixture {
            require(!includeMaterialTextureSlots || !emissiveOnlyMaterial) {
                "Material texture slot fixture and emissive-only fixture are mutually exclusive."
            }
            val suffix = when {
                emissiveOnlyMaterial -> "emissive-only"
                includeMaterialTextureSlots -> "material-slots"
                !writeBuffer -> "missing-buffer"
                !writeImages -> "missing-image"
                else -> "basic"
            }
            val dir = "/tmp/kengine-3d-gltf-test-${getpid()}-$suffix"
            system("mkdir -p $dir")
            val modelPath = "$dir/triangle.gltf"
            val bufferPath = "$dir/triangle mesh.bin"
            val texturePath = "$dir/texture albedo.png"
            val secondaryTexturePaths = if (includeMaterialTextureSlots) {
                mapOf(
                    "normal" to "$dir/normal map.png",
                    "metallicRoughness" to "$dir/metallic roughness map.png",
                    "emissive" to "$dir/emissive map.png",
                    "occlusion" to "$dir/occlusion map.png",
                    "specular" to "$dir/specular map.png"
                )
            } else {
                emptyMap()
            }
            val pbrTextureProperties = if (includeMaterialTextureSlots) {
                """
                          "metallicRoughnessTexture": { "index": 2 }
                """.trimIndent()
            } else {
                ""
            }
            val pbrProperties = if (emissiveOnlyMaterial) {
                """
                          "baseColorFactor": [0.0, 0.0, 0.0, 1.0]
                """.trimIndent()
            } else {
                """
                          "baseColorFactor": [0.5, 0.75, 1.0, 1.0],
                          "baseColorTexture": { "index": 0 }
                          ${pbrTextureProperties.prependCommaIfNotBlank()}
                """.trimIndent()
            }
            val materialTextureProperties = when {
                emissiveOnlyMaterial -> {
                    """
                        "emissiveFactor": [1.0, 0.5, 0.25],
                        "emissiveTexture": { "index": 0 }
                    """.trimIndent()
                }
                includeMaterialTextureSlots -> {
                    """
                        "normalTexture": { "index": 1 },
                        "emissiveTexture": { "index": 3 },
                        "occlusionTexture": { "index": 4 },
                        "extensions": {
                          "KHR_materials_specular": {
                            "specularColorTexture": { "index": 5 }
                          }
                        }
                    """.trimIndent()
                }
                else -> ""
            }
            val texturesJson = if (includeMaterialTextureSlots) {
                """
                      "textures": [
                        { "source": 0 },
                        { "source": 1 },
                        { "source": 2 },
                        { "source": 3 },
                        { "source": 4 },
                        { "source": 5 }
                      ],
                """.trimIndent()
            } else {
                """                      "textures": [{ "source": 0 }],"""
            }
            val imagesJson = if (includeMaterialTextureSlots) {
                """
                      "images": [
                        { "uri": "texture%20albedo.png" },
                        { "uri": "normal%20map.png" },
                        { "uri": "metallic%20roughness%20map.png" },
                        { "uri": "emissive%20map.png" },
                        { "uri": "occlusion%20map.png" },
                        { "uri": "specular%20map.png" }
                      ],
                """.trimIndent()
            } else {
                """                      "images": [{ "uri": "texture%20albedo.png" }],"""
            }

            if (writeBuffer) {
                writeBytes(bufferPath, triangleBuffer())
            }
            if (writeImages) {
                writeBytes(texturePath, byteArrayOf(0))
                secondaryTexturePaths.values.forEach { path ->
                    writeBytes(path, byteArrayOf(0))
                }
            }
            writeText(
                path = modelPath,
                text = """
                    {
                      "asset": { "version": "2.0" },
                      "scene": 0,
                      "scenes": [{ "nodes": [0] }],
                      "nodes": [{ "mesh": 0 }],
                      "meshes": [{
                        "primitives": [{
                          "attributes": {
                            "POSITION": 0,
                            "NORMAL": 1,
                            "TEXCOORD_0": 2
                          },
                          "indices": 3,
                          "material": 0
                        }]
                      }],
                      "materials": [{
                        "name": "Fixture material",
                        "pbrMetallicRoughness": {
                          $pbrProperties
                        }
                        ${materialTextureProperties.prependCommaIfNotBlank()}
                      }],
                      $texturesJson
                      $imagesJson
                      "buffers": [{
                        "uri": "triangle%20mesh.bin",
                        "byteLength": 102
                      }],
                      "bufferViews": [
                        { "buffer": 0, "byteOffset": 0, "byteLength": 36 },
                        { "buffer": 0, "byteOffset": 36, "byteLength": 36 },
                        { "buffer": 0, "byteOffset": 72, "byteLength": 24 },
                        { "buffer": 0, "byteOffset": 96, "byteLength": 6 }
                      ],
                      "accessors": [
                        { "bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3" },
                        { "bufferView": 1, "componentType": 5126, "count": 3, "type": "VEC3" },
                        { "bufferView": 2, "componentType": 5126, "count": 3, "type": "VEC2" },
                        { "bufferView": 3, "componentType": 5123, "count": 3, "type": "SCALAR" }
                      ]
                    }
                """.trimIndent()
            )

            return GltfFixture(
                modelPath = modelPath,
                bufferPath = bufferPath,
                texturePath = texturePath,
                secondaryTexturePaths = secondaryTexturePaths
            )
        }

        private fun triangleBuffer(): ByteArray {
            val bytes = ByteArray(102)
            var offset = 0
            listOf(
                0f, 0f, 0f,
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f,
                1f, 0f,
                0f, 1f
            ).forEach { value ->
                writeFloatLE(bytes, offset, value)
                offset += 4
            }
            writeUnsignedShortLE(bytes, offset, 0)
            writeUnsignedShortLE(bytes, offset + 2, 1)
            writeUnsignedShortLE(bytes, offset + 4, 2)
            return bytes
        }

        private fun writeFloatLE(
            bytes: ByteArray,
            offset: Int,
            value: Float
        ) {
            val bits = value.toBits()
            bytes[offset] = (bits and 0xff).toByte()
            bytes[offset + 1] = ((bits ushr 8) and 0xff).toByte()
            bytes[offset + 2] = ((bits ushr 16) and 0xff).toByte()
            bytes[offset + 3] = ((bits ushr 24) and 0xff).toByte()
        }

        private fun writeUnsignedShortLE(
            bytes: ByteArray,
            offset: Int,
            value: Int
        ) {
            bytes[offset] = (value and 0xff).toByte()
            bytes[offset + 1] = ((value ushr 8) and 0xff).toByte()
        }

        @OptIn(ExperimentalForeignApi::class)
        private fun writeText(
            path: String,
            text: String
        ) {
            writeBytes(path, text.encodeToByteArray())
        }

        @OptIn(ExperimentalForeignApi::class)
        private fun writeBytes(
            path: String,
            bytes: ByteArray
        ) {
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

private fun String.prependCommaIfNotBlank(): String {
    if (isBlank()) {
        return ""
    }
    return ",\n$this"
}
