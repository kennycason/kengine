package com.kengine.sdl

import com.kengine.graphics.alphaFromRGBA
import com.kengine.graphics.blueFromRGBA
import com.kengine.graphics.greenFromRGBA
import com.kengine.graphics.redFromRGBA
import com.kengine.hooks.context.Context
import com.kengine.log.Logger
import com.kengine.log.Logging
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.exit
import sdl3.SDL_BLENDMODE_BLEND
import sdl3.SDL_BLENDMODE_NONE
import sdl3.SDL_CreateRenderer
import sdl3.SDL_CreateWindow
import sdl3.SDL_DestroyRenderer
import sdl3.SDL_DestroyWindow
import sdl3.SDL_GetError
import sdl3.SDL_INIT_VIDEO
import sdl3.SDL_Init
import sdl3.SDL_Quit
import sdl3.SDL_RenderClear
import sdl3.SDL_RenderPresent
import sdl3.SDL_SetRenderDrawBlendMode
import sdl3.SDL_SetRenderDrawColor
import sdl3.SDL_WINDOW_RESIZABLE

@OptIn(ExperimentalForeignApi::class)
class SDLContext private constructor(
    val title: String,
    val screenWidth: Int,
    val screenHeight: Int,
    flags: ULong = SDL_WINDOW_RESIZABLE
) : Context(), Logging {

    private val window: CValuesRef<cnames.structs.SDL_Window> by lazy {
        SDL_CreateWindow(title, screenWidth, screenHeight, flags)
            ?: throw IllegalStateException("Error creating window: ${SDL_GetError()?.toKString()}")
    }

    val renderer: CValuesRef<cnames.structs.SDL_Renderer>? by lazy {
        SDL_CreateRenderer(window, null)
            ?: throw IllegalStateException("Error creating renderer: ${SDL_GetError()?.toKString()}")
    }

    private var currentBlendMode = SDL_BLENDMODE_NONE // SDL3 defaults to NONE

    init {
        require(SDL_Init(SDL_INIT_VIDEO)) {
            Companion.logger.error("Error initializing SDL Video: ${SDL_GetError()?.toKString()}")
            exit(1)
        }
    }


//    private fun initVulkan() {
//        // Load Vulkan library through SDL
//        if (SDL_Vulkan_LoadLibrary(null) != 0) {
//            throw IllegalStateException("Failed to load Vulkan library: ${SDL_GetError()?.toKString()}")
//        }
//
//        // Get required extensions
//        val extensions = memScoped {
//            var count = 0u
//            if (!SDL_Vulkan_GetInstanceExtensions(window, count.ptr, null)) {
//                throw IllegalStateException("Failed to get Vulkan extensions count: ${SDL_GetError()?.toKString()}")
//            }
//            val extensionArray = allocArray<CPointerVar<ByteVar>>(count.toInt())
//            if (!SDL_Vulkan_GetInstanceExtensions(window, count.ptr, extensionArray)) {
//                throw IllegalStateException("Failed to get Vulkan extensions: ${SDL_GetError()?.toKString()}")
//            }
//            List(count.toInt()) { extensionArray[it]!!.toKString() }
//        }
//
//        // Create Vulkan Instance
//        memScoped {
//            val appInfo = cValue<VkApplicationInfo> {
//                sType = VK_STRUCTURE_TYPE_APPLICATION_INFO
//                pApplicationName = "Kengine Vulkan".cstr.ptr
//                applicationVersion = VK_MAKE_VERSION(1, 0, 0)
//                pEngineName = "Kengine".cstr.ptr
//                engineVersion = VK_MAKE_VERSION(1, 0, 0)
//                apiVersion = VK_API_VERSION_1_0
//            }
//
//            val createInfo = cValue<VkInstanceCreateInfo> {
//                sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
//                pApplicationInfo = appInfo.ptr
//                enabledExtensionCount = extensions.size.toUInt()
//                ppEnabledExtensionNames = extensions.toCStringArray(this)
//            }
//
//            val instanceVar = alloc<VkInstanceVar>()
//            if (vkCreateInstance(createInfo.ptr, null, instanceVar.ptr) != VK_SUCCESS) {
//                throw IllegalStateException("Failed to create Vulkan instance")
//            }
//            vkInstance = instanceVar.value
//        }
//
//        // Create Vulkan Surface
//        vkSurface = memScoped {
//            val surface = alloc<VkSurfaceKHRVar>()
//            if (!SDL_Vulkan_CreateSurface(window, vkInstance!!, surface.ptr)) {
//                throw IllegalStateException("Failed to create Vulkan surface: ${SDL_GetError()?.toKString()}")
//            }
//            surface.value
//        }
//
//        logger.info { "Vulkan initialized successfully." }
//    }

    fun enableBlendedMode() {
        setBlendMode(SDL_BLENDMODE_BLEND)
    }

    fun disableBlendedMode() {
        setBlendMode(SDL_BLENDMODE_NONE)
    }

    private fun setBlendMode(mode: UInt) {
        if (currentBlendMode != mode) {
            SDL_SetRenderDrawBlendMode(renderer, mode)
            currentBlendMode = mode
        }
    }

    fun fillScreen(r: UInt, g: UInt, b: UInt, a: UInt = 0xFFu) {
        SDL_SetRenderDrawColor(renderer, r.toUByte(), g.toUByte(), b.toUByte(), a.toUByte())
        SDL_RenderClear(renderer)
    }

    fun fillScreenRGB(rgb: UInt) {
        val r = redFromRGBA(rgb)
        val g = greenFromRGBA(rgb)
        val b = blueFromRGBA(rgb)

        SDL_SetRenderDrawColor(renderer, r, g, b, 0xFFu)
        SDL_RenderClear(renderer)
    }

    fun fillScreen(rgba: UInt) {
        val r = redFromRGBA(rgba)
        val g = greenFromRGBA(rgba)
        val b = blueFromRGBA(rgba)
        val a = alphaFromRGBA(rgba)

        SDL_SetRenderDrawColor(renderer, r, g, b, a)
        SDL_RenderClear(renderer)
    }

    fun flipScreen() {
        SDL_RenderPresent(renderer)
    }

    override fun cleanup() {
        logger.info { "Cleaning up SDLContext"}
        SDL_DestroyRenderer(renderer)
        SDL_DestroyWindow(window)
        SDL_Quit()
        currentContext = null
    }

    companion object {
        private val logger = Logger.get(SDLContext::class)
        private var currentContext: SDLContext? = null

        fun create(
            title: String,
            width: Int,
            height: Int,
            flags: ULong = SDL_WINDOW_RESIZABLE
        ): SDLContext {
            if (currentContext != null) {
               throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }

            currentContext = SDLContext(title, width, height, flags)
            return currentContext!!
        }

        fun get(): SDLContext {
            return currentContext ?: throw IllegalStateException("SDL3Context has not been created. Call create() first.")
        }
    }
}
