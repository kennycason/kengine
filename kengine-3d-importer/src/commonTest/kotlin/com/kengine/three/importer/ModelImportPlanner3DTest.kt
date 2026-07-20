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
        assertFalse(glb.requiresConversion)
        assertNull(glb.outputPath)
        assertEquals(ModelImportFormat3D.OBJ, obj.inputFormat)
        assertEquals(ModelImportAction3D.LOAD_DIRECTLY, obj.action)
    }

    @Test
    fun fbxPlansGlbConversionBesideInput() {
        val plan = ModelImportPlanner3D.plan("assets/source/vehicle.fbx")

        assertEquals(ModelImportFormat3D.FBX, plan.inputFormat)
        assertEquals(ModelImportAction3D.CONVERT_TO_GLB, plan.action)
        assertFalse(plan.runtimeReady)
        assertTrue(plan.requiresConversion)
        assertEquals("assets/source/vehicle.glb", plan.outputPath)
    }

    @Test
    fun usdzCanUseExplicitGlbOutput() {
        val plan = ModelImportPlanner3D.plan(
            inputPath = "assets/source/scene.usdz",
            outputPath = "assets/runtime/scene.glb"
        )

        assertEquals(ModelImportFormat3D.USDZ, plan.inputFormat)
        assertEquals(ModelImportAction3D.CONVERT_TO_GLB, plan.action)
        assertEquals("assets/runtime/scene.glb", plan.outputPath)
    }

    @Test
    fun conversionOutputMustBeGlb() {
        assertFailsWith<IllegalArgumentException> {
            ModelImportPlanner3D.plan(
                inputPath = "assets/source/scene.fbx",
                outputPath = "assets/runtime/scene.gltf"
            )
        }
    }

    @Test
    fun unsupportedFormatExplainsRuntimeAndConversionFormats() {
        val plan = ModelImportPlanner3D.plan("assets/source/scene.blend")

        assertNull(plan.inputFormat)
        assertEquals(ModelImportAction3D.UNSUPPORTED, plan.action)
        assertTrue(plan.message.contains("Runtime formats: glb, gltf, obj."))
        assertTrue(plan.message.contains("Conversion candidates: fbx, usd, usda, usdc, usdz."))
    }
}
