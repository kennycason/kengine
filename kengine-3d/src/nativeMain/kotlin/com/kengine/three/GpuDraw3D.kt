package com.kengine.three

import cnames.structs.SDL_GPUBuffer
import cnames.structs.SDL_GPUGraphicsPipeline
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import sdl3.SDL_BindGPUFragmentSamplers
import sdl3.SDL_BindGPUGraphicsPipeline
import sdl3.SDL_BindGPUVertexBuffers
import sdl3.SDL_DrawGPUPrimitives
import sdl3.SDL_GPUBufferBinding
import sdl3.SDL_GPUTextureSamplerBinding
import sdl3.SDL_PushGPUFragmentUniformData
import sdl3.SDL_PushGPUVertexUniformData

@OptIn(ExperimentalForeignApi::class)
internal data class GpuVertexBufferDrawBinding3D(
    val buffer: CPointer<SDL_GPUBuffer>,
    val slot: UInt = 0u,
    val offsetBytes: UInt = 0u
)

internal data class GpuFragmentTextureDrawBinding3D(
    val texture: GpuTexture,
    val slot: UInt = 0u
)

@OptIn(ExperimentalForeignApi::class)
internal fun GpuFrame.pushVertexUniformFloats3D(
    values: FloatArray,
    slot: UInt = 0u
) {
    pushUniformFloats3D(values) { bytes, byteCount ->
        SDL_PushGPUVertexUniformData(commandBuffer, slot, bytes, byteCount)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuFrame.pushFragmentUniformFloats3D(
    values: FloatArray,
    slot: UInt = 0u
) {
    pushUniformFloats3D(values) { bytes, byteCount ->
        SDL_PushGPUFragmentUniformData(commandBuffer, slot, bytes, byteCount)
    }
}

@OptIn(ExperimentalForeignApi::class)
private inline fun pushUniformFloats3D(
    values: FloatArray,
    push: (CPointer<UByteVar>, UInt) -> Unit
) {
    require(values.isNotEmpty()) {
        "GPU uniform float data must not be empty."
    }

    values.usePinned { pinned ->
        push(
            pinned.addressOf(0).reinterpret(),
            (values.size * Float.SIZE_BYTES).toUInt()
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuFrame.drawPrimitives3D(
    pipeline: CPointer<SDL_GPUGraphicsPipeline>,
    vertexCount: UInt,
    vertexBuffer: GpuVertexBufferDrawBinding3D? = null,
    fragmentTexture: GpuFragmentTextureDrawBinding3D? = null,
    instanceCount: UInt = 1u,
    firstVertex: UInt = 0u,
    firstInstance: UInt = 0u
) {
    require(vertexCount > 0u) {
        "GPU draw vertex count must be greater than zero."
    }
    require(instanceCount > 0u) {
        "GPU draw instance count must be greater than zero."
    }

    memScoped {
        val nativeVertexBinding = vertexBuffer?.let { binding ->
            alloc<SDL_GPUBufferBinding>().also {
                it.buffer = binding.buffer
                it.offset = binding.offsetBytes
            }
        }

        val nativeTextureBinding = fragmentTexture?.let { binding ->
            alloc<SDL_GPUTextureSamplerBinding>().also {
                it.texture = binding.texture.texture
                it.sampler = binding.texture.sampler
            }
        }

        SDL_BindGPUGraphicsPipeline(renderPass, pipeline)
        if (vertexBuffer != null && nativeVertexBinding != null) {
            SDL_BindGPUVertexBuffers(renderPass, vertexBuffer.slot, nativeVertexBinding.ptr, 1u)
        }
        if (fragmentTexture != null && nativeTextureBinding != null) {
            SDL_BindGPUFragmentSamplers(renderPass, fragmentTexture.slot, nativeTextureBinding.ptr, 1u)
        }
        SDL_DrawGPUPrimitives(renderPass, vertexCount, instanceCount, firstVertex, firstInstance)
    }
}
