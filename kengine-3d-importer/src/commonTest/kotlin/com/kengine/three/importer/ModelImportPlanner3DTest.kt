package com.kengine.three.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelImportPlanner3DTest {
    @Test
    fun runtimeReadyFormatsLoadDirectly() {
        val glb = ModelImportPlanner3D.plan("assets/models/world.GLB")
        val obj = ModelImportPlanner3D.plan("C:\\models\\ship.obj")

        assertEquals(ModelImportFormat3D.GLB, glb.inputFormat)
        assertEquals(ModelImportAction3D.LOAD_DIRECTLY, glb.action)
        assertTrue(glb.runtimeReady)
        assertFalse(glb.requiresExternalExport)
        assertNull(glb.suggestedRuntimePath)
        assertEquals(ModelImportFormat3D.OBJ, obj.inputFormat)
        assertEquals(ModelImportAction3D.LOAD_DIRECTLY, obj.action)
    }

    @Test
    fun fbxRequiresExternalGlbExportBesideInput() {
        val plan = ModelImportPlanner3D.plan("assets/source/vehicle.fbx")

        assertEquals(ModelImportFormat3D.FBX, plan.inputFormat)
        assertEquals(ModelImportAction3D.EXTERNAL_EXPORT_REQUIRED, plan.action)
        assertFalse(plan.runtimeReady)
        assertTrue(plan.requiresExternalExport)
        assertEquals("assets/source/vehicle.glb", plan.suggestedRuntimePath)
    }

    @Test
    fun usdzCanUseExplicitGlbOutput() {
        val plan = ModelImportPlanner3D.plan(
            inputPath = "assets/source/scene.usdz",
            suggestedRuntimePath = "assets/runtime/scene.glb"
        )

        assertEquals(ModelImportFormat3D.USDZ, plan.inputFormat)
        assertEquals(ModelImportAction3D.EXTERNAL_EXPORT_REQUIRED, plan.action)
        assertEquals("assets/runtime/scene.glb", plan.suggestedRuntimePath)
    }

    @Test
    fun suggestedRuntimeOutputMustBeGlb() {
        assertFailsWith<IllegalArgumentException> {
            ModelImportPlanner3D.plan(
                inputPath = "assets/source/scene.fbx",
                suggestedRuntimePath = "assets/runtime/scene.gltf"
            )
        }
    }

    @Test
    fun unsupportedFormatExplainsRuntimeAndExternalExportFormats() {
        val plan = ModelImportPlanner3D.plan("assets/source/scene.blend")

        assertNull(plan.inputFormat)
        assertEquals(ModelImportAction3D.UNSUPPORTED, plan.action)
        assertTrue(plan.message.contains("Runtime formats: glb, gltf, obj."))
        assertTrue(plan.message.contains("Source formats that require external GLB export: fbx, usd, usda, usdc, usdz."))
    }
}
