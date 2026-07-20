package com.kengine.three

import cnames.structs.SDL_GPUDevice
import cnames.structs.SDL_GPUTexture
import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import com.kengine.sdl.RenderBackend
import com.kengine.sdl.SDLContext
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import sdl3.SDL_AcquireGPUCommandBuffer
import sdl3.SDL_BeginGPURenderPass
import sdl3.SDL_ClaimWindowForGPUDevice
import sdl3.SDL_CreateGPUDevice
import sdl3.SDL_CreateGPUTexture
import sdl3.SDL_DestroyGPUDevice
import sdl3.SDL_EndGPURenderPass
import sdl3.SDL_GPUColorTargetInfo
import sdl3.SDL_GPUDepthStencilTargetInfo
import sdl3.SDL_GPULoadOp
import sdl3.SDL_GPUSampleCount
import sdl3.SDL_GPUShaderFormat
import sdl3.SDL_GPUStoreOp
import sdl3.SDL_GPUTextureCreateInfo
import sdl3.SDL_GPUTextureFormat
import sdl3.SDL_GPUTextureType
import sdl3.SDL_GPU_TEXTUREUSAGE_DEPTH_STENCIL_TARGET
import sdl3.SDL_GetError
import sdl3.SDL_GetGPUDeviceDriver
import sdl3.SDL_GetGPUShaderFormats
import sdl3.SDL_GetGPUSwapchainTextureFormat
import sdl3.SDL_ReleaseGPUTexture
import sdl3.SDL_ReleaseWindowFromGPUDevice
import sdl3.SDL_SubmitGPUCommandBuffer
import sdl3.SDL_WaitAndAcquireGPUSwapchainTexture

@OptIn(ExperimentalForeignApi::class)
class GpuContext private constructor(
    private val sdl: SDLContext,
    private val debugMode: Boolean
) : Context(), Logging {

    val device: CPointer<SDL_GPUDevice>
    val deviceDriver: String
    val supportedShaderFormats: List<GpuShaderFormat3D>
    val swapchainTextureFormat: SDL_GPUTextureFormat
    val depthTextureFormat: SDL_GPUTextureFormat = SDL_GPUTextureFormat.SDL_GPU_TEXTUREFORMAT_D16_UNORM
    private var cleanedUp = false
    private var depthTexture: CPointer<SDL_GPUTexture>? = null
    private var depthTextureWidth: UInt = 0u
    private var depthTextureHeight: UInt = 0u

    init {
        check(sdl.renderBackend == RenderBackend.SDL_GPU_3D) {
            "GpuContext requires SDLContext renderBackend=${RenderBackend.SDL_GPU_3D}."
        }

        val createdDevice = SDL_CreateGPUDevice(defaultShaderFormats, debugMode, null)
            ?: throw IllegalStateException("Error creating SDL GPU device: ${SDL_GetError()?.toKString()}")

        if (!SDL_ClaimWindowForGPUDevice(createdDevice, sdl.window)) {
            SDL_DestroyGPUDevice(createdDevice)
            throw IllegalStateException("Error claiming SDL window for GPU device: ${SDL_GetError()?.toKString()}")
        }

        device = createdDevice
        deviceDriver = SDL_GetGPUDeviceDriver(device)?.toKString() ?: "unknown"
        supportedShaderFormats = GpuShaderFormat3D.fromMask(SDL_GetGPUShaderFormats(device))
        swapchainTextureFormat = SDL_GetGPUSwapchainTextureFormat(device, sdl.window)
    }

    fun clear(
        r: Float = 0.05f,
        g: Float = 0.06f,
        b: Float = 0.08f,
        a: Float = 1f
    ) {
        render(r, g, b, a) {}
    }

    fun render(
        r: Float = 0.05f,
        g: Float = 0.06f,
        b: Float = 0.08f,
        a: Float = 1f,
        enableDepth: Boolean = false,
        block: (GpuFrame) -> Unit
    ) {
        check(!cleanedUp) {
            "GpuContext has already been cleaned up."
        }

        memScoped {
            val commandBuffer = SDL_AcquireGPUCommandBuffer(device)
                ?: throw IllegalStateException("Error acquiring SDL GPU command buffer: ${SDL_GetError()?.toKString()}")

            val swapchainTexture = alloc<CPointerVar<SDL_GPUTexture>>()
            val swapchainWidth = alloc<UIntVar>()
            val swapchainHeight = alloc<UIntVar>()

            if (!SDL_WaitAndAcquireGPUSwapchainTexture(
                    commandBuffer,
                    sdl.window,
                    swapchainTexture.ptr,
                    swapchainWidth.ptr,
                    swapchainHeight.ptr
                )
            ) {
                throw IllegalStateException("Error acquiring SDL GPU swapchain texture: ${SDL_GetError()?.toKString()}")
            }

            swapchainTexture.value?.let { texture ->
                val colorTarget = alloc<SDL_GPUColorTargetInfo>()
                colorTarget.texture = texture
                colorTarget.mip_level = 0u
                colorTarget.layer_or_depth_plane = 0u
                colorTarget.clear_color.r = r
                colorTarget.clear_color.g = g
                colorTarget.clear_color.b = b
                colorTarget.clear_color.a = a
                colorTarget.load_op = SDL_GPULoadOp.SDL_GPU_LOADOP_CLEAR
                colorTarget.store_op = SDL_GPUStoreOp.SDL_GPU_STOREOP_STORE
                colorTarget.resolve_texture = null
                colorTarget.resolve_mip_level = 0u
                colorTarget.resolve_layer = 0u
                colorTarget.cycle = false
                colorTarget.cycle_resolve_texture = false

                val depthTarget = if (enableDepth) {
                    val depth = alloc<SDL_GPUDepthStencilTargetInfo>()
                    depth.texture = ensureDepthTexture(swapchainWidth.value, swapchainHeight.value)
                    depth.clear_depth = 1.0f
                    depth.load_op = SDL_GPULoadOp.SDL_GPU_LOADOP_CLEAR
                    depth.store_op = SDL_GPUStoreOp.SDL_GPU_STOREOP_DONT_CARE
                    depth.stencil_load_op = SDL_GPULoadOp.SDL_GPU_LOADOP_DONT_CARE
                    depth.stencil_store_op = SDL_GPUStoreOp.SDL_GPU_STOREOP_DONT_CARE
                    depth.cycle = true
                    depth.clear_stencil = 0u
                    depth.mip_level = 0u
                    depth.layer = 0u
                    depth.ptr
                } else {
                    null
                }

                val renderPass = SDL_BeginGPURenderPass(commandBuffer, colorTarget.ptr, 1u, depthTarget)
                    ?: throw IllegalStateException("Error beginning SDL GPU render pass: ${SDL_GetError()?.toKString()}")
                block(
                    GpuFrame(
                        commandBuffer = commandBuffer,
                        renderPass = renderPass,
                        width = swapchainWidth.value,
                        height = swapchainHeight.value
                    )
                )
                SDL_EndGPURenderPass(renderPass)
            }

            if (!SDL_SubmitGPUCommandBuffer(commandBuffer)) {
                throw IllegalStateException("Error submitting SDL GPU command buffer: ${SDL_GetError()?.toKString()}")
            }
        }
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        logger.info { "Cleaning up GpuContext" }
        cleanedUp = true
        depthTexture?.let { SDL_ReleaseGPUTexture(device, it) }
        depthTexture = null
        SDL_ReleaseWindowFromGPUDevice(device, sdl.window)
        SDL_DestroyGPUDevice(device)
        if (currentContext === this) {
            currentContext = null
        }
    }

    private fun ensureDepthTexture(width: UInt, height: UInt): CPointer<SDL_GPUTexture> {
        if (depthTexture != null && depthTextureWidth == width && depthTextureHeight == height) {
            return depthTexture!!
        }

        depthTexture?.let { SDL_ReleaseGPUTexture(device, it) }

        return memScoped {
            val createInfo = alloc<SDL_GPUTextureCreateInfo>()
            createInfo.type = SDL_GPUTextureType.SDL_GPU_TEXTURETYPE_2D
            createInfo.format = depthTextureFormat
            createInfo.usage = SDL_GPU_TEXTUREUSAGE_DEPTH_STENCIL_TARGET
            createInfo.width = width
            createInfo.height = height
            createInfo.layer_count_or_depth = 1u
            createInfo.num_levels = 1u
            createInfo.sample_count = SDL_GPUSampleCount.SDL_GPU_SAMPLECOUNT_1
            createInfo.props = 0u

            SDL_CreateGPUTexture(device, createInfo.ptr)
                ?.also {
                    depthTexture = it
                    depthTextureWidth = width
                    depthTextureHeight = height
                }
                ?: throw IllegalStateException("Error creating SDL GPU depth texture: ${SDL_GetError()?.toKString()}")
        }
    }

    companion object {
        private val defaultShaderFormats: SDL_GPUShaderFormat =
            GpuShaderFormat3D.defaultRequestMask

        private var currentContext: GpuContext? = null

        fun create(
            sdl: SDLContext,
            debugMode: Boolean = true
        ): GpuContext {
            if (currentContext != null) {
                throw IllegalStateException("GpuContext has already been created. Call cleanup() before creating a new context.")
            }

            return GpuContext(sdl, debugMode).also {
                currentContext = it
            }
        }

        fun get(): GpuContext {
            return currentContext ?: throw IllegalStateException("GpuContext has not been created. Call create() first.")
        }
    }
}
