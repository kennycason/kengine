package com.kengine.three

import sdl3.SDL_GPU_SHADERFORMAT_DXIL
import sdl3.SDL_GPU_SHADERFORMAT_MSL
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GpuShader3DTest {
    @Test
    fun shaderFormatsParseSdlFormatMasks() {
        assertEquals(
            listOf(GpuShaderFormat3D.MSL, GpuShaderFormat3D.DXIL),
            GpuShaderFormat3D.fromMask(SDL_GPU_SHADERFORMAT_MSL or SDL_GPU_SHADERFORMAT_DXIL)
        )
        assertEquals(
            listOf(GpuShaderFormat3D.METALLIB, GpuShaderFormat3D.MSL, GpuShaderFormat3D.SPIRV, GpuShaderFormat3D.DXIL),
            GpuShaderFormat3D.fromMask(GpuShaderFormat3D.defaultRequestMask)
        )
    }

    @Test
    fun generatedShaderProgramsKeepRendererResourceDeclarations() {
        assertEquals("primitive", Kengine3DShaderPrograms.PRIMITIVE.label)
        assertEquals(1u, Kengine3DShaderPrograms.PRIMITIVE.vertex.uniformBuffers)
        assertEquals(0u, Kengine3DShaderPrograms.PRIMITIVE.fragment.uniformBuffers)
        assertEquals(0u, Kengine3DShaderPrograms.PRIMITIVE.fragment.samplers)

        assertEquals("textured lit mesh", Kengine3DShaderPrograms.TEXTURED_LIT.label)
        assertEquals(1u, Kengine3DShaderPrograms.TEXTURED_LIT.vertex.uniformBuffers)
        assertEquals(1u, Kengine3DShaderPrograms.TEXTURED_LIT.fragment.uniformBuffers)
        assertEquals(1u, Kengine3DShaderPrograms.TEXTURED_LIT.fragment.samplers)

        assertEquals("skinned textured lit mesh", Kengine3DShaderPrograms.SKINNED_TEXTURED_LIT.label)
        assertEquals(1u, Kengine3DShaderPrograms.SKINNED_TEXTURED_LIT.vertex.uniformBuffers)
        assertEquals(1u, Kengine3DShaderPrograms.SKINNED_TEXTURED_LIT.fragment.uniformBuffers)
        assertEquals(1u, Kengine3DShaderPrograms.SKINNED_TEXTURED_LIT.fragment.samplers)
    }

    @Test
    fun shaderArtifactFactoriesPreserveFormatEntrypointAndBytes() {
        val bytes = byteArrayOf(1, 2, 3)
        val spirv = GpuShaderArtifact3D.spirv(bytes, entrypoint = "vertexMain")
        val dxil = GpuShaderArtifact3D.dxil(bytes, entrypoint = "fragmentMain")

        assertEquals(GpuShaderFormat3D.SPIRV, spirv.format)
        assertEquals("vertexMain", spirv.entrypoint)
        assertContentEquals(bytes, spirv.code)
        assertEquals(GpuShaderFormat3D.DXIL, dxil.format)
        assertEquals("fragmentMain", dxil.entrypoint)
        assertContentEquals(bytes, dxil.code)
    }

    @Test
    fun generatedShaderProgramsIncludeShaderSource() {
        val skinnedVertex = Kengine3DShaderPrograms.SKINNED_TEXTURED_LIT.vertex.artifacts
            .single { it.format == GpuShaderFormat3D.MSL }
        val debugFragment = Kengine3DShaderPrograms.DEBUG.fragment.artifacts
            .single { it.format == GpuShaderFormat3D.MSL }

        assertEquals(GpuShaderFormat3D.MSL, skinnedVertex.format)
        assertEquals(GpuShaderFormat3D.MSL, debugFragment.format)
        assertTrue(skinnedVertex.code.decodeToString().contains("skinMatrices[128]"))
        assertTrue(debugFragment.code.decodeToString().contains("fragment FragmentOut main0"))
    }

    @Test
    fun generatedShaderProgramsCanIncludeCompiledMetalLibraries() {
        val compiledArtifacts = Kengine3DShaderPrograms.PRIMITIVE.vertex.artifacts
            .filter { it.format == GpuShaderFormat3D.METALLIB }

        compiledArtifacts.forEach { artifact ->
            assertEquals("main0", artifact.entrypoint)
            assertTrue(artifact.code.isNotEmpty())
        }
    }
}
