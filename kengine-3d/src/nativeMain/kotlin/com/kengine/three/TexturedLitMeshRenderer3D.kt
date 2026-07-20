package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUGraphicsPipeline

@OptIn(ExperimentalForeignApi::class)
class TexturedLitMeshRenderer3D(
    private val gpu: GpuContext
) {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private var cleanedUp = false

    init {
        pipeline = gpu.createRendererPipeline3D(Kengine3DRendererPresets.TEXTURED_LIT)
    }

    fun draw(
        frame: GpuFrame,
        mesh: TexturedLitGpuMesh,
        texture: GpuTexture,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D(),
        normalTexture: GpuTexture = texture,
        useNormalTexture: Boolean = false
    ) {
        draw(
            frame = frame,
            mesh = mesh,
            texture = texture,
            modelMatrix = transform.matrix(),
            camera = camera,
            light = light,
            normalTexture = normalTexture,
            useNormalTexture = useNormalTexture
        )
    }

    fun draw(
        frame: GpuFrame,
        mesh: TexturedLitGpuMesh,
        texture: GpuTexture,
        modelMatrix: Mat4,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D(),
        normalTexture: GpuTexture = texture,
        useNormalTexture: Boolean = false
    ) {
        check(!cleanedUp) {
            "TexturedLitMeshRenderer3D has already been cleaned up."
        }

        val aspect = frame.width.toFloat() / frame.height.toFloat()
        frame.pushVertexUniformFloats3D(modelAndModelViewProjectionUniforms3D(aspect, modelMatrix, camera))
        frame.pushFragmentUniformFloats3D(texturedDirectionalLightUniforms3D(light, useNormalTexture))
        frame.drawPrimitives3D(
            pipeline = pipeline,
            vertexCount = mesh.vertexCount,
            vertexBuffer = GpuVertexBufferDrawBinding3D(mesh.vertexBuffer),
            fragmentTextures = listOf(
                GpuFragmentTextureDrawBinding3D(texture, slot = 0u),
                GpuFragmentTextureDrawBinding3D(normalTexture, slot = 1u)
            )
        )
    }

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        SDL_ReleaseGPUGraphicsPipeline(gpu.device, pipeline)
    }

}
