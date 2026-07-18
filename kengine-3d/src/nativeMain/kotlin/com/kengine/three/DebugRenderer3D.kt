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
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
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
import sdl3.SDL_GPUVertexAttribute
import sdl3.SDL_GPUVertexBufferDescription
import sdl3.SDL_GPUVertexElementFormat
import sdl3.SDL_GPUVertexInputRate
import sdl3.SDL_GPU_SHADERFORMAT_MSL
import sdl3.SDL_GetError
import sdl3.SDL_PushGPUVertexUniformData
import sdl3.SDL_ReleaseGPUGraphicsPipeline
import sdl3.SDL_ReleaseGPUShader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalForeignApi::class)
class DebugRenderer3D(
    private val gpu: GpuContext
) {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private val lineMesh = GpuMesh.create(gpu, lineVertices())
    private val sphereMesh = GpuMesh.create(gpu, unitWireSphereVertices())
    private val boxMesh = GpuMesh.create(gpu, unitWireBoxVertices())
    private var cleanedUp = false

    init {
        val vertexShader = createShader(DEBUG_VERTEX_MSL, SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX)
        val fragmentShader = createShader(DEBUG_FRAGMENT_MSL, SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_FRAGMENT)

        pipeline = try {
            createPipeline(vertexShader, fragmentShader)
        } finally {
            SDL_ReleaseGPUShader(gpu.device, vertexShader)
            SDL_ReleaseGPUShader(gpu.device, fragmentShader)
        }
    }

    fun line(
        frame: GpuFrame,
        camera: Camera3D,
        start: Vec3,
        end: Vec3,
        color: Color
    ) {
        draw(frame, lineMesh, lineMatrix(start, end), camera, color)
    }

    fun ray(
        frame: GpuFrame,
        camera: Camera3D,
        origin: Vec3,
        direction: Vec3,
        length: Double,
        color: Color
    ) {
        val unitDirection = normalize(direction)
        line(
            frame = frame,
            camera = camera,
            start = origin,
            end = add(origin, scale(unitDirection, length)),
            color = color
        )
    }

    fun wireSphere(
        frame: GpuFrame,
        camera: Camera3D,
        center: Vec3,
        radius: Double,
        color: Color
    ) {
        draw(
            frame = frame,
            mesh = sphereMesh,
            modelMatrix = Mat4.translation(center) * Mat4.scale(Vec3(radius, radius, radius)),
            camera = camera,
            color = color
        )
    }

    fun wireCapsule(
        frame: GpuFrame,
        camera: Camera3D,
        capsule: CapsuleCollider3D,
        color: Color
    ) {
        wireSphere(frame, camera, capsule.start, capsule.radius, color)
        wireSphere(frame, camera, capsule.end, capsule.radius, color)
        line(frame, camera, capsule.start, capsule.end, color)

        val axis = normalize(subtract(capsule.end, capsule.start))
        val reference = if (abs(axis.y) < 0.88) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val right = normalize(cross(reference, axis))
        val forward = normalize(cross(axis, right))
        listOf(right, scale(right, -1.0), forward, scale(forward, -1.0)).forEach { side ->
            val offset = scale(side, capsule.radius)
            line(
                frame = frame,
                camera = camera,
                start = add(capsule.start, offset),
                end = add(capsule.end, offset),
                color = color
            )
        }
    }

    fun wireAabb(
        frame: GpuFrame,
        camera: Camera3D,
        aabb: AabbCollider3D,
        color: Color
    ) {
        val size = Vec3(
            aabb.max.x - aabb.min.x,
            aabb.max.y - aabb.min.y,
            aabb.max.z - aabb.min.z
        )
        val center = Vec3(
            aabb.min.x + size.x * 0.5,
            aabb.min.y + size.y * 0.5,
            aabb.min.z + size.z * 0.5
        )
        draw(
            frame = frame,
            mesh = boxMesh,
            modelMatrix = Mat4.translation(center) * Mat4.scale(size),
            camera = camera,
            color = color
        )
    }

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        lineMesh.cleanup()
        sphereMesh.cleanup()
        boxMesh.cleanup()
        SDL_ReleaseGPUGraphicsPipeline(gpu.device, pipeline)
    }

    private fun draw(
        frame: GpuFrame,
        mesh: GpuMesh,
        modelMatrix: Mat4,
        camera: Camera3D,
        color: Color
    ) {
        check(!cleanedUp) {
            "DebugRenderer3D has already been cleaned up."
        }

        val aspect = frame.width.toFloat() / frame.height.toFloat()
        val modelViewProjection = camera.viewProjection(aspect) * modelMatrix
        val uniforms = FloatArray(20)
        modelViewProjection.values.copyInto(uniforms, destinationOffset = 0)
        uniforms[16] = color.r.toFloat() / 255f
        uniforms[17] = color.g.toFloat() / 255f
        uniforms[18] = color.b.toFloat() / 255f
        uniforms[19] = color.a.toFloat() / 255f

        uniforms.usePinned { pinned ->
            SDL_PushGPUVertexUniformData(
                frame.commandBuffer,
                0u,
                pinned.addressOf(0).reinterpret<UByteVar>(),
                (uniforms.size * 4).toUInt()
            )
        }

        memScoped {
            val binding = alloc<SDL_GPUBufferBinding>()
            binding.buffer = mesh.vertexBuffer
            binding.offset = 0u

            SDL_BindGPUGraphicsPipeline(frame.renderPass, pipeline)
            SDL_BindGPUVertexBuffers(frame.renderPass, 0u, binding.ptr, 1u)
            SDL_DrawGPUPrimitives(frame.renderPass, mesh.vertexCount, 1u, 0u, 0u)
        }
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
                    ?: throw IllegalStateException("Error creating debug shader: ${SDL_GetError()?.toKString()}")
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
            vertexBuffer.pitch = Vertex3D.BYTES_PER_VERTEX.toUInt()

            val attributes = allocArray<SDL_GPUVertexAttribute>(1)
            attributes[0].apply {
                buffer_slot = 0u
                format = SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT3
                location = 0u
                offset = 0u
            }

            val createInfo = alloc<SDL_GPUGraphicsPipelineCreateInfo>()
            createInfo.vertex_shader = vertexShader
            createInfo.fragment_shader = fragmentShader
            createInfo.primitive_type = SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_LINELIST
            createInfo.rasterizer_state.enable_depth_clip = true
            createInfo.depth_stencil_state.enable_depth_test = true
            createInfo.depth_stencil_state.enable_depth_write = false
            createInfo.depth_stencil_state.compare_op = SDL_GPUCompareOp.SDL_GPU_COMPAREOP_LESS_OR_EQUAL
            createInfo.vertex_input_state.num_vertex_buffers = 1u
            createInfo.vertex_input_state.vertex_buffer_descriptions = vertexBuffer.ptr
            createInfo.vertex_input_state.num_vertex_attributes = 1u
            createInfo.vertex_input_state.vertex_attributes = attributes
            createInfo.target_info.num_color_targets = 1u
            createInfo.target_info.color_target_descriptions = colorTarget.ptr
            createInfo.target_info.has_depth_stencil_target = true
            createInfo.target_info.depth_stencil_format = gpu.depthTextureFormat
            createInfo.props = 0u

            SDL_CreateGPUGraphicsPipeline(gpu.device, createInfo.ptr)
                ?: throw IllegalStateException("Error creating debug pipeline: ${SDL_GetError()?.toKString()}")
        }
    }

    companion object {
        private const val SEGMENTS = 32
        private val dummyColor = Color.fromHex("ffffff")

        private fun lineVertices(): List<Vertex3D> {
            return listOf(
                Vertex3D(Vec3(0.0, 0.0, 0.0), dummyColor),
                Vertex3D(Vec3(0.0, 1.0, 0.0), dummyColor)
            )
        }

        private fun unitWireSphereVertices(): List<Vertex3D> {
            val vertices = mutableListOf<Vertex3D>()

            fun addRing(point: (Double) -> Vec3) {
                for (index in 0 until SEGMENTS) {
                    val a = (index.toDouble() / SEGMENTS.toDouble()) * kotlin.math.PI * 2.0
                    val b = ((index + 1).toDouble() / SEGMENTS.toDouble()) * kotlin.math.PI * 2.0
                    vertices += Vertex3D(point(a), dummyColor)
                    vertices += Vertex3D(point(b), dummyColor)
                }
            }

            addRing { angle -> Vec3(cos(angle), sin(angle), 0.0) }
            addRing { angle -> Vec3(cos(angle), 0.0, sin(angle)) }
            addRing { angle -> Vec3(0.0, cos(angle), sin(angle)) }
            return vertices
        }

        private fun unitWireBoxVertices(): List<Vertex3D> {
            val corners = listOf(
                Vec3(-0.5, -0.5, -0.5),
                Vec3(0.5, -0.5, -0.5),
                Vec3(0.5, -0.5, 0.5),
                Vec3(-0.5, -0.5, 0.5),
                Vec3(-0.5, 0.5, -0.5),
                Vec3(0.5, 0.5, -0.5),
                Vec3(0.5, 0.5, 0.5),
                Vec3(-0.5, 0.5, 0.5)
            )
            val edges = listOf(
                0 to 1,
                1 to 2,
                2 to 3,
                3 to 0,
                4 to 5,
                5 to 6,
                6 to 7,
                7 to 4,
                0 to 4,
                1 to 5,
                2 to 6,
                3 to 7
            )
            return edges.flatMap { edge ->
                listOf(Vertex3D(corners[edge.first], dummyColor), Vertex3D(corners[edge.second], dummyColor))
            }
        }

        private fun lineMatrix(
            start: Vec3,
            end: Vec3
        ): Mat4 {
            val delta = subtract(end, start)
            return Mat4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    delta.x.toFloat(), delta.y.toFloat(), delta.z.toFloat(), 0f,
                    0f, 0f, 1f, 0f,
                    start.x.toFloat(), start.y.toFloat(), start.z.toFloat(), 1f
                )
            )
        }

        private fun add(
            a: Vec3,
            b: Vec3
        ): Vec3 {
            return Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
        }

        private fun subtract(
            a: Vec3,
            b: Vec3
        ): Vec3 {
            return Vec3(a.x - b.x, a.y - b.y, a.z - b.z)
        }

        private fun scale(
            value: Vec3,
            scale: Double
        ): Vec3 {
            return Vec3(value.x * scale, value.y * scale, value.z * scale)
        }

        private fun cross(
            a: Vec3,
            b: Vec3
        ): Vec3 {
            return Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
            )
        }

        private fun normalize(value: Vec3): Vec3 {
            val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
            if (length <= 0.0000001) {
                return Vec3(0.0, 1.0, 0.0)
            }
            return Vec3(value.x / length, value.y / length, value.z / length)
        }

        private const val DEBUG_VERTEX_MSL = """
#include <metal_stdlib>
#include <simd/simd.h>
using namespace metal;

struct Uniforms
{
    float4x4 modelViewProjection;
    float4 color;
};

struct VertexIn
{
    float3 position [[attribute(0)]];
};

struct VertexOut
{
    float4 color [[user(locn0)]];
    float4 position [[position]];
};

vertex VertexOut main0(VertexIn in [[stage_in]], constant Uniforms& uniforms [[buffer(0)]])
{
    VertexOut out;
    out.color = uniforms.color;
    out.position = uniforms.modelViewProjection * float4(in.position, 1.0);
    return out;
}
"""

        private const val DEBUG_FRAGMENT_MSL = """
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
