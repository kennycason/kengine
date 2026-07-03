package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import cnames.structs.SDL_GPUShader
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import sdl3.SDL_BindGPUFragmentSamplers
import sdl3.SDL_BindGPUGraphicsPipeline
import sdl3.SDL_BindGPUVertexBuffers
import sdl3.SDL_CreateGPUGraphicsPipeline
import sdl3.SDL_CreateGPUShader
import sdl3.SDL_DrawGPUPrimitives
import sdl3.SDL_GPUBufferBinding
import sdl3.SDL_GPUColorTargetDescription
import sdl3.SDL_GPUCompareOp
import sdl3.SDL_GPUGraphicsPipelineCreateInfo
import sdl3.SDL_GPUPrimitiveType
import sdl3.SDL_GPUShaderCreateInfo
import sdl3.SDL_GPUShaderStage
import sdl3.SDL_GPUTextureSamplerBinding
import sdl3.SDL_GPUVertexAttribute
import sdl3.SDL_GPUVertexBufferDescription
import sdl3.SDL_GPUVertexElementFormat
import sdl3.SDL_GPUVertexInputRate
import sdl3.SDL_GPU_SHADERFORMAT_MSL
import sdl3.SDL_GetError
import sdl3.SDL_PushGPUVertexUniformData
import sdl3.SDL_ReleaseGPUGraphicsPipeline
import sdl3.SDL_ReleaseGPUShader

@OptIn(ExperimentalForeignApi::class)
class TexturedMeshRenderer3D(
    private val gpu: GpuContext
) {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private var cleanedUp = false

    init {
        val vertexShader = createShader(TEXTURED_VERTEX_MSL, SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX)
        val fragmentShader = createShader(TEXTURED_FRAGMENT_MSL, SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_FRAGMENT)

        pipeline = try {
            createPipeline(vertexShader, fragmentShader)
        } finally {
            SDL_ReleaseGPUShader(gpu.device, vertexShader)
            SDL_ReleaseGPUShader(gpu.device, fragmentShader)
        }
    }

    fun draw(
        frame: GpuFrame,
        mesh: TexturedGpuMesh,
        texture: GpuTexture,
        transform: Transform3D,
        camera: PerspectiveCamera
    ) {
        draw(frame, mesh, texture, transform.matrix(), camera)
    }

    fun draw(
        frame: GpuFrame,
        mesh: TexturedGpuMesh,
        texture: GpuTexture,
        modelMatrix: Mat4,
        camera: PerspectiveCamera
    ) {
        check(!cleanedUp) {
            "TexturedMeshRenderer3D has already been cleaned up."
        }

        val aspect = frame.width.toFloat() / frame.height.toFloat()
        val modelViewProjection = camera.viewProjection(aspect) * modelMatrix

        modelViewProjection.values.usePinned { pinned ->
            SDL_PushGPUVertexUniformData(
                frame.commandBuffer,
                0u,
                pinned.addressOf(0).reinterpret<UByteVar>(),
                64u
            )
        }

        memScoped {
            val binding = alloc<SDL_GPUBufferBinding>()
            binding.buffer = mesh.vertexBuffer
            binding.offset = 0u

            val textureBinding = alloc<SDL_GPUTextureSamplerBinding>()
            textureBinding.texture = texture.texture
            textureBinding.sampler = texture.sampler

            SDL_BindGPUGraphicsPipeline(frame.renderPass, pipeline)
            SDL_BindGPUVertexBuffers(frame.renderPass, 0u, binding.ptr, 1u)
            SDL_BindGPUFragmentSamplers(frame.renderPass, 0u, textureBinding.ptr, 1u)
            SDL_DrawGPUPrimitives(frame.renderPass, mesh.vertexCount, 1u, 0u, 0u)
        }
    }

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        SDL_ReleaseGPUGraphicsPipeline(gpu.device, pipeline)
    }

    private fun createShader(
        source: String,
        stage: SDL_GPUShaderStage
    ): CPointer<SDL_GPUShader> {
        val bytes = source.encodeToByteArray()

        return memScoped {
            bytes.usePinned { pinned ->
                val createInfo = alloc<SDL_GPUShaderCreateInfo>()
                createInfo.code_size = bytes.size.convert()
                createInfo.code = pinned.addressOf(0).reinterpret<UByteVar>()
                createInfo.entrypoint = "main0".cstr.ptr
                createInfo.format = SDL_GPU_SHADERFORMAT_MSL
                createInfo.stage = stage
                createInfo.num_samplers =
                    if (stage == SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_FRAGMENT) 1u else 0u
                createInfo.num_storage_textures = 0u
                createInfo.num_storage_buffers = 0u
                createInfo.num_uniform_buffers =
                    if (stage == SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX) 1u else 0u
                createInfo.props = 0u

                SDL_CreateGPUShader(gpu.device, createInfo.ptr)
                    ?: throw IllegalStateException("Error creating textured mesh shader: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    private fun createPipeline(
        vertexShader: CPointer<SDL_GPUShader>,
        fragmentShader: CPointer<SDL_GPUShader>
    ): CPointer<SDL_GPUGraphicsPipeline> {
        return memScoped {
            val colorTarget = alloc<SDL_GPUColorTargetDescription>()
            colorTarget.format = gpu.swapchainTextureFormat

            val vertexBuffer = alloc<SDL_GPUVertexBufferDescription>()
            vertexBuffer.slot = 0u
            vertexBuffer.input_rate = SDL_GPUVertexInputRate.SDL_GPU_VERTEXINPUTRATE_VERTEX
            vertexBuffer.instance_step_rate = 0u
            vertexBuffer.pitch = TextureVertex3D.BYTES_PER_VERTEX.toUInt()

            val attributes = allocArray<SDL_GPUVertexAttribute>(2)
            attributes[0].apply {
                buffer_slot = 0u
                format = SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT3
                location = 0u
                offset = 0u
            }
            attributes[1].apply {
                buffer_slot = 0u
                format = SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT2
                location = 1u
                offset = 12u
            }

            val createInfo = alloc<SDL_GPUGraphicsPipelineCreateInfo>()
            createInfo.vertex_shader = vertexShader
            createInfo.fragment_shader = fragmentShader
            createInfo.primitive_type = SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_TRIANGLELIST
            createInfo.rasterizer_state.enable_depth_clip = true
            createInfo.depth_stencil_state.enable_depth_test = true
            createInfo.depth_stencil_state.enable_depth_write = true
            createInfo.depth_stencil_state.compare_op = SDL_GPUCompareOp.SDL_GPU_COMPAREOP_LESS_OR_EQUAL
            createInfo.vertex_input_state.num_vertex_buffers = 1u
            createInfo.vertex_input_state.vertex_buffer_descriptions = vertexBuffer.ptr
            createInfo.vertex_input_state.num_vertex_attributes = 2u
            createInfo.vertex_input_state.vertex_attributes = attributes
            createInfo.target_info.num_color_targets = 1u
            createInfo.target_info.color_target_descriptions = colorTarget.ptr
            createInfo.target_info.has_depth_stencil_target = true
            createInfo.target_info.depth_stencil_format = gpu.depthTextureFormat
            createInfo.props = 0u

            SDL_CreateGPUGraphicsPipeline(gpu.device, createInfo.ptr)
                ?: throw IllegalStateException("Error creating textured mesh pipeline: ${SDL_GetError()?.toKString()}")
        }
    }

    companion object {
        private const val TEXTURED_VERTEX_MSL = """
#include <metal_stdlib>
#include <simd/simd.h>
using namespace metal;

struct Uniforms
{
    float4x4 modelViewProjection;
};

struct VertexIn
{
    float3 position [[attribute(0)]];
    float2 uv [[attribute(1)]];
};

struct VertexOut
{
    float2 uv [[user(locn0)]];
    float4 position [[position]];
};

vertex VertexOut main0(VertexIn in [[stage_in]], constant Uniforms& uniforms [[buffer(0)]])
{
    VertexOut out;
    out.uv = in.uv;
    out.position = uniforms.modelViewProjection * float4(in.position, 1.0);
    return out;
}
"""

        private const val TEXTURED_FRAGMENT_MSL = """
#include <metal_stdlib>
using namespace metal;

struct FragmentIn
{
    float2 uv [[user(locn0)]];
};

struct FragmentOut
{
    float4 color [[color(0)]];
};

fragment FragmentOut main0(
    FragmentIn in [[stage_in]],
    texture2d<float> baseTexture [[texture(0)]],
    sampler baseSampler [[sampler(0)]])
{
    FragmentOut out;
    out.color = baseTexture.sample(baseSampler, in.uv);
    return out;
}
"""
    }
}
