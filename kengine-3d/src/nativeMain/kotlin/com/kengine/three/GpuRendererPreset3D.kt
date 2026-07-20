package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

internal data class GpuRendererPreset3D(
    val shaderProgram: GpuShaderProgramSource3D,
    val pipeline: GpuGraphicsPipelineDescriptor3D
) {
    val label: String = pipeline.label

    init {
        require(shaderProgram.label == pipeline.label) {
            "Renderer preset shader label '${shaderProgram.label}' must match pipeline label '${pipeline.label}'."
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.createRendererPipeline3D(
    preset: GpuRendererPreset3D
): CPointer<SDL_GPUGraphicsPipeline> {
    return withShaderProgram3D(preset.shaderProgram) { shaders ->
        createGraphicsPipeline3D(preset.pipeline, shaders)
    }
}

internal object Kengine3DVertexLayouts {
    val DEBUG_POSITION = GpuVertexInputLayout3D.singleBuffer(
        pitchBytes = Vertex3D.BYTES_PER_VERTEX,
        GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0)
    )

    val VERTEX_COLOR = GpuVertexInputLayout3D.singleBuffer(
        pitchBytes = Vertex3D.BYTES_PER_VERTEX,
        GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0),
        GpuVertexAttribute3D.float3(location = 1, offsetBytes = 12)
    )

    val TEXTURED = GpuVertexInputLayout3D.singleBuffer(
        pitchBytes = TextureVertex3D.BYTES_PER_VERTEX,
        GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0),
        GpuVertexAttribute3D.float2(location = 1, offsetBytes = 12)
    )

    val LIT = GpuVertexInputLayout3D.singleBuffer(
        pitchBytes = LitVertex3D.BYTES_PER_VERTEX,
        GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0),
        GpuVertexAttribute3D.float3(location = 1, offsetBytes = 12),
        GpuVertexAttribute3D.float3(location = 2, offsetBytes = 24)
    )

    val TEXTURED_LIT = GpuVertexInputLayout3D.singleBuffer(
        pitchBytes = TexturedLitVertex3D.BYTES_PER_VERTEX,
        GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0),
        GpuVertexAttribute3D.float3(location = 1, offsetBytes = 12),
        GpuVertexAttribute3D.float3(location = 2, offsetBytes = 24),
        GpuVertexAttribute3D.float2(location = 3, offsetBytes = 36)
    )

    val SKINNED_TEXTURED_LIT = GpuVertexInputLayout3D.singleBuffer(
        pitchBytes = SkinnedTexturedLitVertex3D.BYTES_PER_VERTEX,
        GpuVertexAttribute3D.float3(location = 0, offsetBytes = 0),
        GpuVertexAttribute3D.float3(location = 1, offsetBytes = 12),
        GpuVertexAttribute3D.float3(location = 2, offsetBytes = 24),
        GpuVertexAttribute3D.float2(location = 3, offsetBytes = 36),
        GpuVertexAttribute3D.float4(location = 4, offsetBytes = 44),
        GpuVertexAttribute3D.float4(location = 5, offsetBytes = 60)
    )
}

internal object Kengine3DRendererPresets {
    val PRIMITIVE = Kengine3DShaderPrograms.PRIMITIVE.trianglePreset()

    val MESH = Kengine3DShaderPrograms.MESH.trianglePreset(
        vertexInput = Kengine3DVertexLayouts.VERTEX_COLOR
    )

    val LIT = Kengine3DShaderPrograms.LIT.trianglePreset(
        vertexInput = Kengine3DVertexLayouts.LIT
    )

    val TEXTURED = Kengine3DShaderPrograms.TEXTURED.trianglePreset(
        vertexInput = Kengine3DVertexLayouts.TEXTURED
    )

    val TEXTURED_LIT = Kengine3DShaderPrograms.TEXTURED_LIT.trianglePreset(
        vertexInput = Kengine3DVertexLayouts.TEXTURED_LIT
    )

    val SKINNED_TEXTURED_LIT = Kengine3DShaderPrograms.SKINNED_TEXTURED_LIT.trianglePreset(
        vertexInput = Kengine3DVertexLayouts.SKINNED_TEXTURED_LIT,
        debugContext = listOf("maxSkinJoints=${SkinnedTexturedLitMeshRenderer3D.MAX_SKIN_JOINTS}")
    )

    val DEBUG = Kengine3DShaderPrograms.DEBUG.linePreset(
        vertexInput = Kengine3DVertexLayouts.DEBUG_POSITION
    )

    val ALL = listOf(
        PRIMITIVE,
        MESH,
        LIT,
        TEXTURED,
        TEXTURED_LIT,
        SKINNED_TEXTURED_LIT,
        DEBUG
    )
}

private fun GpuShaderProgramSource3D.trianglePreset(
    vertexInput: GpuVertexInputLayout3D = GpuVertexInputLayout3D.EMPTY,
    enableDepthWrite: Boolean = true,
    debugContext: List<String> = emptyList()
): GpuRendererPreset3D {
    return GpuRendererPreset3D(
        shaderProgram = this,
        pipeline = GpuGraphicsPipelineDescriptor3D.triangleList(
            label = label,
            vertexInput = vertexInput,
            enableDepthWrite = enableDepthWrite,
            debugContext = debugContext
        )
    )
}

private fun GpuShaderProgramSource3D.linePreset(
    vertexInput: GpuVertexInputLayout3D = GpuVertexInputLayout3D.EMPTY,
    enableDepthWrite: Boolean = false,
    debugContext: List<String> = emptyList()
): GpuRendererPreset3D {
    return GpuRendererPreset3D(
        shaderProgram = this,
        pipeline = GpuGraphicsPipelineDescriptor3D.lineList(
            label = label,
            vertexInput = vertexInput,
            enableDepthWrite = enableDepthWrite,
            debugContext = debugContext
        )
    )
}
