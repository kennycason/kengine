package com.kengine.three.importer

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getpid
import platform.posix.system
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelAssetPreflight3DTest {
    @Test
    fun loadableRuntimeObjReturnsInspectedModelInfo() {
        val objPath = writeObjFixture()

        val result = ModelAssetPreflight3D.inspect(objPath)

        assertEquals(ModelAssetPreflightStatus3D.LOADABLE, result.status)
        assertTrue(result.loadable)
        assertEquals(ModelImportAction3D.LOAD_DIRECTLY, result.plan.action)
        val info = assertNotNull(result.modelInfo)
        assertEquals(3, info.vertexCount)
        assertEquals(1, info.primitiveCount)
        assertTrue(result.message.contains("inspected successfully"))
    }

    @Test
    fun missingRuntimeObjReturnsInvalidRuntimeAsset() {
        val missingPath = "/tmp/kengine-3d-importer-test-${getpid()}-missing/missing.obj"

        val result = ModelAssetPreflight3D.inspect(missingPath)

        assertEquals(ModelAssetPreflightStatus3D.INVALID_RUNTIME_ASSET, result.status)
        assertFalse(result.loadable)
        assertEquals(ModelImportAction3D.LOAD_DIRECTLY, result.plan.action)
        assertNull(result.modelInfo)
        assertTrue(result.message.contains("OBJ model file was not found"))
    }

    @Test
    fun fbxRequiresExternalExportWithoutRuntimeInspection() {
        val result = ModelAssetPreflight3D.inspect("assets/source/vehicle.fbx")

        assertEquals(ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED, result.status)
        assertFalse(result.loadable)
        assertEquals(ModelImportAction3D.EXTERNAL_EXPORT_REQUIRED, result.plan.action)
        assertEquals("assets/source/vehicle.glb", result.plan.suggestedRuntimePath)
        assertNull(result.modelInfo)
        assertTrue(result.message.contains("Export it to GLB from your asset tool"))
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun writeObjFixture(): String {
        val dir = "/tmp/kengine-3d-importer-test-${getpid()}-obj"
        system("mkdir -p $dir")
        val objPath = "$dir/triangle.obj"
        writeText(
            path = objPath,
            text = """
                v 0 0 0
                v 1 0 0
                v 0 1 0
                f 1 2 3
            """.trimIndent()
        )
        return objPath
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
