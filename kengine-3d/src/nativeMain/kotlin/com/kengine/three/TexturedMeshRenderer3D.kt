package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUGraphicsPipeline

@OptIn(ExperimentalForeignApi::class)
class TexturedMeshRenderer3D(
    private val gpu: GpuContext
) {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private var cleanedUp = false

    init {
        pipeline = gpu.createRendererPipeline3D(Kengine3DRendererPresets.TEXTURED)
    }

    fun draw(
        frame: GpuFrame,
        mesh: TexturedGpuMesh,
        texture: GpuTexture,
        transform: Transform3D,
        camera: Camera3D
    ) {
        draw(frame, mesh, texture, transform.matrix(), camera)
    }

    fun draw(
        frame: GpuFrame,
        mesh: TexturedGpuMesh,
        texture: GpuTexture,
        modelMatrix: Mat4,
        camera: Camera3D
    ) {
        check(!cleanedUp) {
            "TexturedMeshRenderer3D has already been cleaned up."
        }

        val aspect = frame.width.toFloat() / frame.height.toFloat()
        frame.pushVertexUniformFloats3D(modelViewProjectionUniforms3D(aspect, modelMatrix, camera))
        frame.drawPrimitives3D(
            pipeline = pipeline,
            vertexCount = mesh.vertexCount,
            vertexBuffer = GpuVertexBufferDrawBinding3D(mesh.vertexBuffer),
            fragmentTexture = GpuFragmentTextureDrawBinding3D(texture)
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
