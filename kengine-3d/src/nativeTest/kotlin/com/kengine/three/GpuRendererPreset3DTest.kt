package com.kengine.three

import sdl3.SDL_GPUPrimitiveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class GpuRendererPreset3DTest {
    @Test
    fun builtInPresetsHaveUniqueMatchingShaderAndPipelineLabels() {
        val presets = Kengine3DRendererPresets.ALL

        assertEquals(7, presets.size)
        assertEquals(presets.size, presets.map { it.label }.toSet().size)
        presets.forEach { preset ->
            assertEquals(preset.shaderProgram.label, preset.label)
            assertEquals(preset.pipeline.label, preset.label)
        }
    }

    @Test
    fun presetRejectsMismatchedShaderAndPipelineLabels() {
        assertFailsWith<IllegalArgumentException> {
            GpuRendererPreset3D(
                shaderProgram = Kengine3DShaderPrograms.MESH,
                pipeline = GpuGraphicsPipelineDescriptor3D.triangleList(label = "not mesh")
            )
        }
    }

    @Test
    fun builtInVertexLayoutsMatchExpectedShaderInputs() {
        assertEquals(listOf("attributes=1", "vertexBytes=24"), Kengine3DVertexLayouts.DEBUG_POSITION.errorContext())
        assertEquals(listOf("attributes=2", "vertexBytes=24"), Kengine3DVertexLayouts.VERTEX_COLOR.errorContext())
        assertEquals(listOf("attributes=2", "vertexBytes=20"), Kengine3DVertexLayouts.TEXTURED.errorContext())
        assertEquals(listOf("attributes=3", "vertexBytes=36"), Kengine3DVertexLayouts.LIT.errorContext())
        assertEquals(listOf("attributes=4", "vertexBytes=44"), Kengine3DVertexLayouts.TEXTURED_LIT.errorContext())
        assertEquals(
            listOf("attributes=6", "vertexBytes=76"),
            Kengine3DVertexLayouts.SKINNED_TEXTURED_LIT.errorContext()
        )
    }

    @Test
    fun debugPresetUsesLinePipelineWithoutDepthWrites() {
        val debug = Kengine3DRendererPresets.DEBUG.pipeline

        assertEquals(SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_LINELIST, debug.primitiveType)
        assertFalse(debug.enableDepthWrite)
        assertEquals("attributes=1, vertexBytes=24", debug.errorContext())
    }

    @Test
    fun skinnedPresetCarriesJointLimitDebugContext() {
        val skinned = Kengine3DRendererPresets.SKINNED_TEXTURED_LIT.pipeline

        assertEquals(SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_TRIANGLELIST, skinned.primitiveType)
        assertEquals(
            "attributes=6, vertexBytes=76, maxSkinJoints=${SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS}",
            skinned.errorContext()
        )
    }
}
