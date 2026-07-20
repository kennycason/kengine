package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import cnames.structs.SDL_GPUShader
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl3.SDL_CreateGPUGraphicsPipeline
import sdl3.SDL_GPUColorTargetDescription
import sdl3.SDL_GPUCompareOp
import sdl3.SDL_GPUGraphicsPipelineCreateInfo
import sdl3.SDL_GPUPrimitiveType
import sdl3.SDL_GPUVertexAttribute
import sdl3.SDL_GPUVertexBufferDescription
import sdl3.SDL_GPUVertexElementFormat
import sdl3.SDL_GPUVertexInputRate

internal data class GpuVertexAttribute3D(
    val location: Int,
    val format: SDL_GPUVertexElementFormat,
    val offsetBytes: Int,
    val bufferSlot: Int = 0
) {
    init {
        require(location >= 0) {
            "Vertex attribute location must be non-negative."
        }
        require(offsetBytes >= 0) {
            "Vertex attribute offset must be non-negative."
        }
        require(bufferSlot >= 0) {
            "Vertex attribute buffer slot must be non-negative."
        }
    }

    companion object {
        fun float2(location: Int, offsetBytes: Int, bufferSlot: Int = 0): GpuVertexAttribute3D {
            return GpuVertexAttribute3D(
                location = location,
                format = SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT2,
                offsetBytes = offsetBytes,
                bufferSlot = bufferSlot
            )
        }

        fun float3(location: Int, offsetBytes: Int, bufferSlot: Int = 0): GpuVertexAttribute3D {
            return GpuVertexAttribute3D(
                location = location,
                format = SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT3,
                offsetBytes = offsetBytes,
                bufferSlot = bufferSlot
            )
        }

        fun float4(location: Int, offsetBytes: Int, bufferSlot: Int = 0): GpuVertexAttribute3D {
            return GpuVertexAttribute3D(
                location = location,
                format = SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT4,
                offsetBytes = offsetBytes,
                bufferSlot = bufferSlot
            )
        }
    }
}

internal data class GpuVertexBufferLayout3D(
    val pitchBytes: Int,
    val slot: Int = 0,
    val inputRate: SDL_GPUVertexInputRate = SDL_GPUVertexInputRate.SDL_GPU_VERTEXINPUTRATE_VERTEX,
    val instanceStepRate: Int = 0
) {
    init {
        require(pitchBytes >= 0) {
            "Vertex buffer pitch must be non-negative."
        }
        require(slot >= 0) {
            "Vertex buffer slot must be non-negative."
        }
        require(instanceStepRate >= 0) {
            "Vertex buffer instance step rate must be non-negative."
        }
    }
}

internal data class GpuVertexInputLayout3D(
    val buffers: List<GpuVertexBufferLayout3D> = emptyList(),
    val attributes: List<GpuVertexAttribute3D> = emptyList()
) {
    init {
        val duplicateBufferSlots = buffers
            .groupingBy { it.slot }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateBufferSlots.isEmpty()) {
            "Vertex buffer slots must be unique: ${duplicateBufferSlots.joinToString()}."
        }

        val duplicateAttributeLocations = attributes
            .groupingBy { it.location }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateAttributeLocations.isEmpty()) {
            "Vertex attribute locations must be unique: ${duplicateAttributeLocations.joinToString()}."
        }

        val buffersBySlot = buffers.associateBy { it.slot }
        attributes.forEach { attribute ->
            val buffer = buffersBySlot[attribute.bufferSlot]
            require(buffer != null) {
                "Vertex attribute location ${attribute.location} references missing buffer slot ${attribute.bufferSlot}."
            }
            require(attribute.offsetBytes < buffer.pitchBytes) {
                "Vertex attribute location ${attribute.location} offset ${attribute.offsetBytes} exceeds vertex buffer slot ${buffer.slot} pitch ${buffer.pitchBytes}."
            }
        }
    }

    fun errorContext(): List<String> {
        if (buffers.isEmpty() && attributes.isEmpty()) {
            return emptyList()
        }

        val context = mutableListOf("attributes=${attributes.size}")
        buffers.singleOrNull()?.let { buffer ->
            context += "vertexBytes=${buffer.pitchBytes}"
        } ?: context.add("vertexBuffers=${buffers.size}")
        return context
    }

    companion object {
        val EMPTY = GpuVertexInputLayout3D()

        fun singleBuffer(
            pitchBytes: Int,
            vararg attributes: GpuVertexAttribute3D
        ): GpuVertexInputLayout3D {
            return GpuVertexInputLayout3D(
                buffers = listOf(GpuVertexBufferLayout3D(pitchBytes = pitchBytes)),
                attributes = attributes.toList()
            )
        }
    }
}

internal data class GpuGraphicsPipelineDescriptor3D(
    val label: String,
    val primitiveType: SDL_GPUPrimitiveType,
    val vertexInput: GpuVertexInputLayout3D = GpuVertexInputLayout3D.EMPTY,
    val enableDepthTest: Boolean = true,
    val enableDepthWrite: Boolean = true,
    val depthCompareOp: SDL_GPUCompareOp = SDL_GPUCompareOp.SDL_GPU_COMPAREOP_LESS_OR_EQUAL,
    val enableDepthClip: Boolean = true,
    val debugContext: List<String> = emptyList()
) {
    fun errorContext(): String {
        return (vertexInput.errorContext() + debugContext).joinToString(", ")
    }

    companion object {
        fun triangleList(
            label: String,
            vertexInput: GpuVertexInputLayout3D = GpuVertexInputLayout3D.EMPTY,
            enableDepthWrite: Boolean = true,
            debugContext: List<String> = emptyList()
        ): GpuGraphicsPipelineDescriptor3D {
            return GpuGraphicsPipelineDescriptor3D(
                label = label,
                primitiveType = SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_TRIANGLELIST,
                vertexInput = vertexInput,
                enableDepthWrite = enableDepthWrite,
                debugContext = debugContext
            )
        }

        fun lineList(
            label: String,
            vertexInput: GpuVertexInputLayout3D = GpuVertexInputLayout3D.EMPTY,
            enableDepthWrite: Boolean = false,
            debugContext: List<String> = emptyList()
        ): GpuGraphicsPipelineDescriptor3D {
            return GpuGraphicsPipelineDescriptor3D(
                label = label,
                primitiveType = SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_LINELIST,
                vertexInput = vertexInput,
                enableDepthWrite = enableDepthWrite,
                debugContext = debugContext
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.createGraphicsPipeline3D(
    descriptor: GpuGraphicsPipelineDescriptor3D,
    shaders: GpuShaderProgram3D
): CPointer<SDL_GPUGraphicsPipeline> {
    return createGraphicsPipeline3D(
        descriptor = descriptor,
        vertexShader = shaders.vertexShader,
        fragmentShader = shaders.fragmentShader
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.createGraphicsPipeline3D(
    descriptor: GpuGraphicsPipelineDescriptor3D,
    vertexShader: CPointer<SDL_GPUShader>,
    fragmentShader: CPointer<SDL_GPUShader>
): CPointer<SDL_GPUGraphicsPipeline> {
    return memScoped {
        val colorTarget = alloc<SDL_GPUColorTargetDescription>()
        colorTarget.format = swapchainTextureFormat

        val vertexBuffers = descriptor.vertexInput.buffers
            .takeIf { it.isNotEmpty() }
            ?.let { buffers ->
                allocArray<SDL_GPUVertexBufferDescription>(buffers.size).also { nativeBuffers ->
                    buffers.forEachIndexed { index, buffer ->
                        nativeBuffers[index].apply {
                            slot = buffer.slot.toUInt()
                            input_rate = buffer.inputRate
                            instance_step_rate = buffer.instanceStepRate.toUInt()
                            pitch = buffer.pitchBytes.toUInt()
                        }
                    }
                }
            }

        val vertexAttributes = descriptor.vertexInput.attributes
            .takeIf { it.isNotEmpty() }
            ?.let { attributes ->
                allocArray<SDL_GPUVertexAttribute>(attributes.size).also { nativeAttributes ->
                    attributes.forEachIndexed { index, attribute ->
                        nativeAttributes[index].apply {
                            buffer_slot = attribute.bufferSlot.toUInt()
                            format = attribute.format
                            location = attribute.location.toUInt()
                            offset = attribute.offsetBytes.toUInt()
                        }
                    }
                }
            }

        val createInfo = alloc<SDL_GPUGraphicsPipelineCreateInfo>()
        createInfo.vertex_shader = vertexShader
        createInfo.fragment_shader = fragmentShader
        createInfo.primitive_type = descriptor.primitiveType
        createInfo.rasterizer_state.enable_depth_clip = descriptor.enableDepthClip
        createInfo.depth_stencil_state.enable_depth_test = descriptor.enableDepthTest
        createInfo.depth_stencil_state.enable_depth_write = descriptor.enableDepthWrite
        createInfo.depth_stencil_state.compare_op = descriptor.depthCompareOp
        createInfo.vertex_input_state.num_vertex_buffers = descriptor.vertexInput.buffers.size.toUInt()
        createInfo.vertex_input_state.num_vertex_attributes = descriptor.vertexInput.attributes.size.toUInt()
        vertexBuffers?.let {
            createInfo.vertex_input_state.vertex_buffer_descriptions = it
        }
        vertexAttributes?.let {
            createInfo.vertex_input_state.vertex_attributes = it
        }
        createInfo.target_info.num_color_targets = 1u
        createInfo.target_info.color_target_descriptions = colorTarget.ptr
        createInfo.target_info.has_depth_stencil_target = true
        createInfo.target_info.depth_stencil_format = depthTextureFormat
        createInfo.props = 0u

        SDL_CreateGPUGraphicsPipeline(device, createInfo.ptr)
            ?: throw IllegalStateException(descriptor.pipelineErrorMessage(sdlErrorMessage3D()))
    }
}

private fun GpuGraphicsPipelineDescriptor3D.pipelineErrorMessage(error: String): String {
    val context = errorContext()
    return if (context.isBlank()) {
        "Error creating $label pipeline: $error"
    } else {
        "Error creating $label pipeline ($context): $error"
    }
}
