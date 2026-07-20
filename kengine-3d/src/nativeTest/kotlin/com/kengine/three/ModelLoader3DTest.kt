package com.kengine.three

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
}

private data class GltfFixture(
    val modelPath: String,
    val texturePath: String
) {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun write(): GltfFixture {
            val dir = "/tmp/kengine-3d-gltf-test-${getpid()}"
            system("mkdir -p $dir")
            val modelPath = "$dir/triangle.gltf"
            val bufferPath = "$dir/triangle mesh.bin"
            val texturePath = "$dir/texture albedo.png"

            writeBytes(bufferPath, triangleBuffer())
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
                          "baseColorFactor": [0.5, 0.75, 1.0, 1.0],
                          "baseColorTexture": { "index": 0 }
                        }
                      }],
                      "textures": [{ "source": 0 }],
                      "images": [{ "uri": "texture%20albedo.png" }],
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
                texturePath = texturePath
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
