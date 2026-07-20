package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUGraphicsPipeline

@OptIn(ExperimentalForeignApi::class)
class SkinnedTexturedLitMeshRenderer3D(
    private val gpu: GpuContext
) : GpuResource3D {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private var cleanedUp = false

    init {
        pipeline = gpu.createRendererPipeline3D(Kengine3DRendererPresets.SKINNED_TEXTURED_LIT)
    }

    fun draw(
        frame: GpuFrame,
        mesh: SkinnedTexturedLitGpuMesh,
        texture: GpuTexture,
        transform: Transform3D,
        camera: Camera3D,
        skinMatrices: List<Mat4>,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        draw(frame, mesh, texture, transform.matrix(), camera, skinMatrices, light)
    }

    fun draw(
        frame: GpuFrame,
        mesh: SkinnedTexturedLitGpuMesh,
        texture: GpuTexture,
        modelMatrix: Mat4,
        camera: Camera3D,
        skinMatrices: List<Mat4>,
        light: DirectionalLight3D = DirectionalLight3D()
    ) {
        check(!cleanedUp) {
            "SkinnedTexturedLitMeshRenderer3D has already been cleaned up."
        }
        require(skinMatrices.size <= MAX_SKIN_JOINTS) {
            "SkinnedTexturedLitMeshRenderer3D supports at most $MAX_SKIN_JOINTS skin joints per draw."
        }
        require(mesh.maxJointIndex < skinMatrices.size) {
            "Skinned textured lit mesh references joint ${mesh.maxJointIndex}, but only ${skinMatrices.size} skin matrices were provided."
        }

        val aspect = frame.width.toFloat() / frame.height.toFloat()
        frame.pushVertexUniformFloats3D(
            skinnedModelUniforms3D(
                aspect = aspect,
                modelMatrix = modelMatrix,
                camera = camera,
                skinMatrices = skinMatrices,
                maxSkinJoints = MAX_SKIN_JOINTS
            )
        )
        frame.pushFragmentUniformFloats3D(directionalLightUniforms3D(light))
        frame.drawPrimitives3D(
            pipeline = pipeline,
            vertexCount = mesh.vertexCount,
            vertexBuffer = GpuVertexBufferDrawBinding3D(mesh.vertexBuffer),
            fragmentTexture = GpuFragmentTextureDrawBinding3D(texture)
        )
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        SDL_ReleaseGPUGraphicsPipeline(gpu.device, pipeline)
    }

    companion object {
        const val MAX_SKIN_JOINTS = 128
    }
}
