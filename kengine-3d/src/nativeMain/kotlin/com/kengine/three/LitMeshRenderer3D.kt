package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUGraphicsPipeline

@OptIn(ExperimentalForeignApi::class)
class LitMeshRenderer3D(
    private val gpu: GpuContext
) {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private var cleanedUp = false

    init {
        pipeline = gpu.createRendererPipeline3D(Kengine3DRendererPresets.LIT)
    }

    fun draw(
        frame: GpuFrame,
        mesh: LitGpuMesh,
        transform: Transform3D,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        draw(frame, mesh, transform.matrix(), camera, light)
    }

    fun draw(
        frame: GpuFrame,
        mesh: LitGpuMesh,
        modelMatrix: Mat4,
        camera: Camera3D,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        check(!cleanedUp) {
            "LitMeshRenderer3D has already been cleaned up."
        }

        val aspect = frame.width.toFloat() / frame.height.toFloat()
        frame.pushVertexUniformFloats3D(modelAndModelViewProjectionUniforms3D(aspect, modelMatrix, camera))
        frame.pushFragmentUniformFloats3D(directionalLightUniforms3D(light))
        frame.drawPrimitives3D(
            pipeline = pipeline,
            vertexCount = mesh.vertexCount,
            vertexBuffer = GpuVertexBufferDrawBinding3D(mesh.vertexBuffer)
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
