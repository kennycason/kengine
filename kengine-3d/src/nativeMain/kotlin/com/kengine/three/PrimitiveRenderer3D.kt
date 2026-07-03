package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import cnames.structs.SDL_GPUShader
import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import sdl3.SDL_BindGPUGraphicsPipeline
import sdl3.SDL_CreateGPUGraphicsPipeline
import sdl3.SDL_CreateGPUShader
import sdl3.SDL_DrawGPUPrimitives
import sdl3.SDL_GPUColorTargetDescription
import sdl3.SDL_GPUCompareOp
import sdl3.SDL_GPUGraphicsPipelineCreateInfo
import sdl3.SDL_GPUPrimitiveType
import sdl3.SDL_GPUShaderCreateInfo
import sdl3.SDL_GPUShaderStage
import sdl3.SDL_GPU_SHADERFORMAT_MSL
import sdl3.SDL_GetError
import sdl3.SDL_PushGPUVertexUniformData
import sdl3.SDL_ReleaseGPUGraphicsPipeline
import sdl3.SDL_ReleaseGPUShader

@OptIn(ExperimentalForeignApi::class)
class PrimitiveRenderer3D(
    private val gpu: GpuContext
) {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private var cleanedUp = false

    init {
        val vertexShader = createShader(PRIMITIVE_VERTEX_MSL, SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX)
        val fragmentShader = createShader(PRIMITIVE_FRAGMENT_MSL, SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_FRAGMENT)

        pipeline = try {
            createPipeline(vertexShader, fragmentShader)
        } finally {
            SDL_ReleaseGPUShader(gpu.device, vertexShader)
            SDL_ReleaseGPUShader(gpu.device, fragmentShader)
        }
    }

    fun triangle(
        frame: GpuFrame,
        center: Vec3,
        size: Float,
        color: Color,
        rotationRadians: Float = 0f
    ) {
        drawShape(
            frame = frame,
            center = center,
            width = size,
            height = size,
            color = color,
            rotationRadians = rotationRadians,
            shape = Shape.TRIANGLE
        )
    }

    fun quad(
        frame: GpuFrame,
        center: Vec3,
        width: Float,
        height: Float,
        color: Color,
        rotationRadians: Float = 0f
    ) {
        drawShape(
            frame = frame,
            center = center,
            width = width,
            height = height,
            color = color,
            rotationRadians = rotationRadians,
            shape = Shape.QUAD
        )
    }

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        SDL_ReleaseGPUGraphicsPipeline(gpu.device, pipeline)
    }

    private fun drawShape(
        frame: GpuFrame,
        center: Vec3,
        width: Float,
        height: Float,
        color: Color,
        rotationRadians: Float,
        shape: Shape
    ) {
        check(!cleanedUp) {
            "PrimitiveRenderer3D has already been cleaned up."
        }

        val placement = project(frame, center, width, height)
        val uniforms = floatArrayOf(
            placement.x,
            placement.y,
            placement.depth,
            shape.uniformValue,
            placement.width,
            placement.height,
            rotationRadians,
            0f,
            color.r.toFloat() / 255f,
            color.g.toFloat() / 255f,
            color.b.toFloat() / 255f,
            color.a.toFloat() / 255f
        )

        uniforms.usePinned { pinned ->
            SDL_PushGPUVertexUniformData(
                frame.commandBuffer,
                0u,
                pinned.addressOf(0).reinterpret<UByteVar>(),
                (uniforms.size * 4).toUInt()
            )
        }

        SDL_BindGPUGraphicsPipeline(frame.renderPass, pipeline)
        SDL_DrawGPUPrimitives(frame.renderPass, shape.vertexCount, 1u, 0u, 0u)
    }

    private fun project(frame: GpuFrame, center: Vec3, width: Float, height: Float): ProjectedShape {
        val distance = (-center.z.toFloat()).coerceIn(NEAR_Z, FAR_Z)
        val aspect = frame.width.toFloat() / frame.height.toFloat()
        val clipX = (center.x.toFloat() / distance) / aspect
        val clipY = center.y.toFloat() / distance
        val clipDepth = ((distance - NEAR_Z) / (FAR_Z - NEAR_Z)).coerceIn(0f, 1f)
        val clipWidth = (width / distance) / aspect
        val clipHeight = height / distance

        return ProjectedShape(
            x = clipX,
            y = clipY,
            depth = clipDepth,
            width = clipWidth,
            height = clipHeight
        )
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
                createInfo.num_samplers = 0u
                createInfo.num_storage_textures = 0u
                createInfo.num_storage_buffers = 0u
                createInfo.num_uniform_buffers =
                    if (stage == SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX) 1u else 0u
                createInfo.props = 0u

                SDL_CreateGPUShader(gpu.device, createInfo.ptr)
                    ?: throw IllegalStateException("Error creating primitive shader: ${SDL_GetError()?.toKString()}")
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

            val createInfo = alloc<SDL_GPUGraphicsPipelineCreateInfo>()
            createInfo.vertex_shader = vertexShader
            createInfo.fragment_shader = fragmentShader
            createInfo.primitive_type = SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_TRIANGLELIST
            createInfo.rasterizer_state.enable_depth_clip = true
            createInfo.depth_stencil_state.enable_depth_test = true
            createInfo.depth_stencil_state.enable_depth_write = true
            createInfo.depth_stencil_state.compare_op = SDL_GPUCompareOp.SDL_GPU_COMPAREOP_LESS_OR_EQUAL
            createInfo.target_info.num_color_targets = 1u
            createInfo.target_info.color_target_descriptions = colorTarget.ptr
            createInfo.target_info.has_depth_stencil_target = true
            createInfo.target_info.depth_stencil_format = gpu.depthTextureFormat
            createInfo.props = 0u

            SDL_CreateGPUGraphicsPipeline(gpu.device, createInfo.ptr)
                ?: throw IllegalStateException("Error creating primitive pipeline: ${SDL_GetError()?.toKString()}")
        }
    }

    private data class ProjectedShape(
        val x: Float,
        val y: Float,
        val depth: Float,
        val width: Float,
        val height: Float
    )

    private enum class Shape(
        val uniformValue: Float,
        val vertexCount: UInt
    ) {
        TRIANGLE(0f, 3u),
        QUAD(1f, 6u)
    }

    companion object {
        private const val NEAR_Z = 0.35f
        private const val FAR_Z = 8f

        private const val PRIMITIVE_VERTEX_MSL = """
#include <metal_stdlib>
using namespace metal;

struct VertexOut
{
    float4 position [[position]];
    float4 color [[user(locn0)]];
};

vertex VertexOut main0(uint vertexID [[vertex_id]], constant float4* uniforms [[buffer(0)]])
{
    const float2 trianglePositions[3] = {
        float2(0.0, 0.66),
        float2(-0.58, -0.44),
        float2(0.58, -0.44)
    };

    const float2 quadPositions[6] = {
        float2(-0.5, 0.5),
        float2(0.5, 0.5),
        float2(-0.5, -0.5),
        float2(0.5, 0.5),
        float2(0.5, -0.5),
        float2(-0.5, -0.5)
    };

    const float4 placement = uniforms[0];
    const float4 scaleRotation = uniforms[1];
    const float4 color = uniforms[2];

    float2 local = placement.w > 0.5 ? quadPositions[vertexID] : trianglePositions[vertexID];
    float c = cos(scaleRotation.z);
    float s = sin(scaleRotation.z);
    float2 rotated = float2(
        local.x * c - local.y * s,
        local.x * s + local.y * c
    );

    VertexOut out;
    out.position = float4(
        placement.x + rotated.x * scaleRotation.x,
        placement.y + rotated.y * scaleRotation.y,
        placement.z,
        1.0
    );
    out.color = color;
    return out;
}
"""

        private const val PRIMITIVE_FRAGMENT_MSL = """
#include <metal_stdlib>
using namespace metal;

struct FragmentIn
{
    float4 color [[user(locn0)]];
};

struct FragmentOut
{
    float4 color [[color(0)]];
};

fragment FragmentOut main0(FragmentIn in [[stage_in]])
{
    FragmentOut out;
    out.color = in.color;
    return out;
}
"""
    }
}
