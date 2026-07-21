package com.kengine.three.ui

import cnames.structs.SDL_GPUGraphicsPipeline
import cnames.structs.SDL_GPUShader
import com.kengine.font.Font
import com.kengine.graphics.Color
import com.kengine.three.GpuContext
import com.kengine.three.GpuFrame
import com.kengine.three.GpuResource3D
import com.kengine.three.GpuShaderFormat3D
import com.kengine.three.GpuTexture
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.size_t
import sdl3.SDL_BindGPUFragmentSamplers
import sdl3.SDL_BindGPUGraphicsPipeline
import sdl3.SDL_CreateGPUGraphicsPipeline
import sdl3.SDL_CreateGPUShader
import sdl3.SDL_DrawGPUPrimitives
import sdl3.SDL_GPUBlendFactor
import sdl3.SDL_GPUBlendOp
import sdl3.SDL_GPUColorTargetDescription
import sdl3.SDL_GPUCompareOp
import sdl3.SDL_GPUGraphicsPipelineCreateInfo
import sdl3.SDL_GPUPrimitiveType
import sdl3.SDL_GPUShaderCreateInfo
import sdl3.SDL_GPUShaderStage
import sdl3.SDL_GPUTextureSamplerBinding
import sdl3.SDL_GPU_SHADERFORMAT_MSL
import sdl3.SDL_GetError
import sdl3.SDL_PushGPUFragmentUniformData
import sdl3.SDL_PushGPUVertexUniformData
import sdl3.SDL_ReleaseGPUGraphicsPipeline
import sdl3.SDL_ReleaseGPUShader
import sdl3.ttf.SDL_Color
import sdl3.ttf.SDL_ConvertSurface
import sdl3.ttf.SDL_DestroySurface
import sdl3.ttf.SDL_PIXELFORMAT_RGBA32
import sdl3.ttf.SDL_Surface
import sdl3.ttf.TTF_RenderText_Blended
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalForeignApi::class)
class GpuUiRenderer3D(
    private val gpu: GpuContext,
    private val font: Font
) : GpuResource3D {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline> = createPipeline()
    private val whiteTexture = GpuTexture.createRgba8(
        gpu = gpu,
        width = 1u,
        height = 1u,
        pixels = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
    )
    private val coinTexture = GpuTexture.createRgba8(
        gpu = gpu,
        width = COIN_TEXTURE_SIZE.toUInt(),
        height = COIN_TEXTURE_SIZE.toUInt(),
        pixels = coinTexturePixels(COIN_TEXTURE_SIZE)
    )
    private val textTextures = mutableMapOf<TextKey, TextTexture>()
    private var cleanedUp = false

    fun render(
        ui: GpuUiContext3D,
        frame: GpuFrame
    ) {
        renderFrame(frame) {
            ui.render(this, frame)
        }
    }

    fun preloadText(text: String) {
        if (text.isNotBlank()) {
            ensureTextTexture(text)
        }
    }

    fun rect(
        rect: GpuUiRect3D,
        color: Color,
        frameWidth: UInt,
        frameHeight: UInt
    ) {
        drawTexture(
            texture = whiteTexture,
            rect = rect,
            color = color,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
    }

    fun coin(
        rect: GpuUiRect3D,
        color: Color,
        frameWidth: UInt,
        frameHeight: UInt
    ) {
        drawTexture(
            texture = coinTexture,
            rect = rect,
            color = color,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
    }

    fun text(
        text: String,
        rect: GpuUiRect3D,
        color: Color,
        align: GpuUiAlign3D,
        verticalAlign: GpuUiVerticalAlign3D,
        frameWidth: UInt,
        frameHeight: UInt
    ) {
        if (text.isBlank()) {
            return
        }

        val textTexture = cachedTextTexture(text)
        val textRect = alignedTextRect(
            container = rect,
            textWidth = textTexture.width.toDouble(),
            textHeight = textTexture.height.toDouble(),
            align = align,
            verticalAlign = verticalAlign
        )
        drawTexture(
            texture = textTexture.texture,
            rect = textRect,
            color = color,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        textTextures.values.forEach { it.texture.cleanup() }
        textTextures.clear()
        whiteTexture.cleanup()
        coinTexture.cleanup()
        SDL_ReleaseGPUGraphicsPipeline(gpu.device, pipeline)
    }

    private fun drawTexture(
        texture: GpuTexture,
        rect: GpuUiRect3D,
        color: Color,
        frameWidth: UInt,
        frameHeight: UInt
    ) {
        check(!cleanedUp) {
            "GpuUiRenderer3D has already been cleaned up."
        }

        val vertexUniforms = floatArrayOf(
            rect.x.toFloat(),
            rect.y.toFloat(),
            rect.width.toFloat(),
            rect.height.toFloat(),
            frameWidth.toFloat(),
            frameHeight.toFloat(),
            0f,
            0f
        )
        val fragmentUniforms = floatArrayOf(
            color.r.toFloat() / 255f,
            color.g.toFloat() / 255f,
            color.b.toFloat() / 255f,
            color.a.toFloat() / 255f
        )

        activeFrame?.let { frame ->
            frame.pushVertexFloats(vertexUniforms)
            frame.pushFragmentFloats(fragmentUniforms)
            frame.drawUiQuad(texture)
        } ?: error("GpuUiRenderer3D draw calls must be made during renderFrame.")
    }

    private var activeFrame: GpuFrame? = null

    internal fun renderFrame(
        frame: GpuFrame,
        draw: () -> Unit
    ) {
        check(activeFrame == null) {
            "GpuUiRenderer3D does not support nested renderFrame calls."
        }
        activeFrame = frame
        try {
            draw()
        } finally {
            activeFrame = null
        }
    }

    private fun GpuFrame.drawUiQuad(texture: GpuTexture) {
        memScoped {
            val textureBinding = alloc<SDL_GPUTextureSamplerBinding>()
            textureBinding.texture = texture.texture
            textureBinding.sampler = texture.sampler

            SDL_BindGPUGraphicsPipeline(renderPass, pipeline)
            SDL_BindGPUFragmentSamplers(renderPass, 0u, textureBinding.ptr, 1u)
            SDL_DrawGPUPrimitives(renderPass, 6u, 1u, 0u, 0u)
        }
    }

    private fun GpuFrame.pushVertexFloats(values: FloatArray) {
        values.usePinned { pinned ->
            SDL_PushGPUVertexUniformData(
                commandBuffer,
                0u,
                pinned.addressOf(0).reinterpret<UByteVar>(),
                (values.size * Float.SIZE_BYTES).toUInt()
            )
        }
    }

    private fun GpuFrame.pushFragmentFloats(values: FloatArray) {
        values.usePinned { pinned ->
            SDL_PushGPUFragmentUniformData(
                commandBuffer,
                0u,
                pinned.addressOf(0).reinterpret<UByteVar>(),
                (values.size * Float.SIZE_BYTES).toUInt()
            )
        }
    }

    private fun cachedTextTexture(text: String): TextTexture {
        val key = textKey(text)
        return textTextures[key] ?: if (activeFrame == null) {
            ensureTextTexture(text)
        } else {
            error("GPU UI text '$text' was not preloaded before rendering.")
        }
    }

    private fun ensureTextTexture(text: String): TextTexture {
        val key = textKey(text)
        return textTextures.getOrPut(key) {
            createTextTexture(text)
        }
    }

    private fun textKey(text: String): TextKey {
        return TextKey(
            text = text,
            fontName = font.name,
            fontSize = font.fontSize
        )
    }

    private fun createTextTexture(text: String): TextTexture {
        val textByteLength = text.encodeToByteArray().size
        val surface = TTF_RenderText_Blended(
            font = font.font,
            text = text,
            length = textByteLength.convert<size_t>(),
            fg = whiteSdlColor()
        ) ?: throw IllegalStateException("Failed to render GPU UI text '$text': ${sdlError()}")

        return try {
            createTextureFromSurface(surface, "text:$text")
        } finally {
            SDL_DestroySurface(surface)
        }
    }

    private fun createTextureFromSurface(
        surface: CPointer<SDL_Surface>,
        label: String
    ): TextTexture {
        val converted = SDL_ConvertSurface(surface, SDL_PIXELFORMAT_RGBA32)
            ?: throw IllegalStateException("Failed to convert GPU UI text surface '$label': ${sdlError()}")

        try {
            val width = converted.pointed.w.toUInt()
            val height = converted.pointed.h.toUInt()
            val pitch = converted.pointed.pitch
            val pixels = converted.pointed.pixels
                ?: throw IllegalStateException("GPU UI text surface has no pixel data: $label")
            val source = pixels.reinterpret<ByteVar>()
            val bytes = ByteArray((width * height * 4u).toInt())

            for (y in 0 until height.toInt()) {
                val sourceRow = y * pitch
                val destinationRow = y * width.toInt() * 4
                for (x in 0 until width.toInt() * 4) {
                    bytes[destinationRow + x] = source[sourceRow + x]
                }
            }

            return TextTexture(
                texture = GpuTexture.createRgba8(
                    gpu = gpu,
                    width = width,
                    height = height,
                    pixels = bytes
                ),
                width = width,
                height = height
            )
        } finally {
            SDL_DestroySurface(converted)
        }
    }

    private fun alignedTextRect(
        container: GpuUiRect3D,
        textWidth: Double,
        textHeight: Double,
        align: GpuUiAlign3D,
        verticalAlign: GpuUiVerticalAlign3D
    ): GpuUiRect3D {
        val x = when (align) {
            GpuUiAlign3D.LEFT -> container.x
            GpuUiAlign3D.CENTER -> container.x + (container.width - textWidth) * 0.5
            GpuUiAlign3D.RIGHT -> container.x + container.width - textWidth
        }
        val y = when (verticalAlign) {
            GpuUiVerticalAlign3D.TOP -> container.y
            GpuUiVerticalAlign3D.CENTER -> container.y + (container.height - textHeight) * 0.5
            GpuUiVerticalAlign3D.BOTTOM -> container.y + container.height - textHeight
        }
        return GpuUiRect3D(
            x = x,
            y = y,
            width = textWidth,
            height = textHeight
        )
    }

    private fun createPipeline(): CPointer<SDL_GPUGraphicsPipeline> {
        if (GpuShaderFormat3D.MSL !in gpu.supportedShaderFormats) {
            throw IllegalStateException(
                "GpuUiRenderer3D currently requires MSL shader support. " +
                    "Driver '${gpu.deviceDriver}' supports ${gpu.supportedShaderFormats.joinToString { it.displayName }}."
            )
        }

        val vertexShader = createShader(
            stage = SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX,
            source = vertexShaderSource,
            uniformBuffers = 1u,
            samplers = 0u
        )
        val fragmentShader = try {
            createShader(
                stage = SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_FRAGMENT,
                source = fragmentShaderSource,
                uniformBuffers = 1u,
                samplers = 1u
            )
        } catch (e: Throwable) {
            SDL_ReleaseGPUShader(gpu.device, vertexShader)
            throw e
        }

        return try {
            createPipeline(vertexShader, fragmentShader)
        } finally {
            SDL_ReleaseGPUShader(gpu.device, fragmentShader)
            SDL_ReleaseGPUShader(gpu.device, vertexShader)
        }
    }

    private fun createShader(
        stage: SDL_GPUShaderStage,
        source: String,
        uniformBuffers: UInt,
        samplers: UInt
    ): CPointer<SDL_GPUShader> {
        val code = source.encodeToByteArray()
        return memScoped {
            code.usePinned { pinned ->
                val createInfo = alloc<SDL_GPUShaderCreateInfo>()
                createInfo.code_size = code.size.convert()
                createInfo.code = pinned.addressOf(0).reinterpret<UByteVar>()
                createInfo.entrypoint = "main0".cstr.ptr
                createInfo.format = SDL_GPU_SHADERFORMAT_MSL
                createInfo.stage = stage
                createInfo.num_samplers = samplers
                createInfo.num_storage_textures = 0u
                createInfo.num_storage_buffers = 0u
                createInfo.num_uniform_buffers = uniformBuffers
                createInfo.props = 0u

                SDL_CreateGPUShader(gpu.device, createInfo.ptr)
                    ?: throw IllegalStateException("Failed to create GPU UI shader: ${sdlError()}")
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
            colorTarget.blend_state.enable_blend = true
            colorTarget.blend_state.color_write_mask = 0xFu
            colorTarget.blend_state.color_blend_op = SDL_GPUBlendOp.SDL_GPU_BLENDOP_ADD
            colorTarget.blend_state.alpha_blend_op = SDL_GPUBlendOp.SDL_GPU_BLENDOP_ADD
            colorTarget.blend_state.src_color_blendfactor = SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_SRC_ALPHA
            colorTarget.blend_state.dst_color_blendfactor = SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ONE_MINUS_SRC_ALPHA
            colorTarget.blend_state.src_alpha_blendfactor = SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_SRC_ALPHA
            colorTarget.blend_state.dst_alpha_blendfactor = SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ONE_MINUS_SRC_ALPHA

            val createInfo = alloc<SDL_GPUGraphicsPipelineCreateInfo>()
            createInfo.vertex_shader = vertexShader
            createInfo.fragment_shader = fragmentShader
            createInfo.primitive_type = SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_TRIANGLELIST
            createInfo.rasterizer_state.enable_depth_clip = false
            createInfo.depth_stencil_state.enable_depth_test = false
            createInfo.depth_stencil_state.enable_depth_write = false
            createInfo.depth_stencil_state.compare_op = SDL_GPUCompareOp.SDL_GPU_COMPAREOP_ALWAYS
            createInfo.vertex_input_state.num_vertex_buffers = 0u
            createInfo.vertex_input_state.num_vertex_attributes = 0u
            createInfo.target_info.num_color_targets = 1u
            createInfo.target_info.color_target_descriptions = colorTarget.ptr
            createInfo.target_info.has_depth_stencil_target = true
            createInfo.target_info.depth_stencil_format = gpu.depthTextureFormat
            createInfo.props = 0u

            SDL_CreateGPUGraphicsPipeline(gpu.device, createInfo.ptr)
                ?: throw IllegalStateException("Failed to create GPU UI pipeline: ${sdlError()}")
        }
    }

    private fun whiteSdlColor() = memScoped {
        alloc<SDL_Color>().apply {
            r = 255u.toUByte()
            g = 255u.toUByte()
            b = 255u.toUByte()
            a = 255u.toUByte()
        }.readValue()
    }

    private fun sdlError(): String {
        return SDL_GetError()?.toKString()?.takeIf { it.isNotBlank() } ?: "unknown SDL error"
    }

    private data class TextKey(
        val text: String,
        val fontName: String,
        val fontSize: Float
    )

    private data class TextTexture(
        val texture: GpuTexture,
        val width: UInt,
        val height: UInt
    )

    companion object {
        private const val COIN_TEXTURE_SIZE = 64

        private fun coinTexturePixels(size: Int): ByteArray {
            val bytes = ByteArray(size * size * 4)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val normalizedX = ((x + 0.5) / size.toDouble()) * 2.0 - 1.0
                    val normalizedY = ((y + 0.5) / size.toDouble()) * 2.0 - 1.0
                    val distance = sqrt(normalizedX * normalizedX + normalizedY * normalizedY)
                    val index = (y * size + x) * 4
                    if (distance >= 1.0) {
                        bytes[index + 3] = 0
                        continue
                    }

                    val alpha = if (distance > 0.92) {
                        ((1.0 - distance) / 0.08).coerceIn(0.0, 1.0)
                    } else {
                        1.0
                    }
                    val rim = if (distance > 0.72) 0.22 else 0.0
                    val innerGroove = if (abs(distance - 0.48) < 0.035) -0.16 else 0.0
                    val diagonalHighlight = ((-normalizedX - normalizedY) * 0.12).coerceIn(-0.08, 0.14)
                    val brightness = (0.74 + rim + innerGroove + diagonalHighlight).coerceIn(0.0, 1.0)

                    bytes[index] = byteColor(brightness)
                    bytes[index + 1] = byteColor(brightness)
                    bytes[index + 2] = byteColor(brightness)
                    bytes[index + 3] = byteAlpha(alpha)
                }
            }
            return bytes
        }

        private fun byteColor(value: Double): Byte {
            return (value * 255.0).toInt().coerceIn(0, 255).toByte()
        }

        private fun byteAlpha(value: Double): Byte {
            return (value * 255.0).toInt().coerceIn(0, 255).toByte()
        }

        private val vertexShaderSource = """
            #include <metal_stdlib>
            using namespace metal;

            struct VertexOut
            {
                float4 position [[position]];
                float2 uv [[user(locn0)]];
            };

            vertex VertexOut main0(uint vertexID [[vertex_id]], constant float4* uniforms [[buffer(0)]])
            {
                const float2 quadPositions[6] = {
                    float2(0.0, 0.0),
                    float2(1.0, 0.0),
                    float2(0.0, 1.0),
                    float2(1.0, 0.0),
                    float2(1.0, 1.0),
                    float2(0.0, 1.0)
                };

                const float4 rect = uniforms[0];
                const float4 viewport = uniforms[1];
                float2 local = quadPositions[vertexID];
                float2 pixel = rect.xy + local * rect.zw;
                float2 clip = float2(
                    (pixel.x / viewport.x) * 2.0 - 1.0,
                    1.0 - (pixel.y / viewport.y) * 2.0
                );

                VertexOut out;
                out.position = float4(clip, 0.0, 1.0);
                out.uv = local;
                return out;
            }
        """.trimIndent()

        private val fragmentShaderSource = """
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
                constant float4& color [[buffer(0)]],
                texture2d<float> baseTexture [[texture(0)]],
                sampler baseSampler [[sampler(0)]])
            {
                FragmentOut out;
                out.color = baseTexture.sample(baseSampler, in.uv) * color;
                return out;
            }
        """.trimIndent()
    }
}
