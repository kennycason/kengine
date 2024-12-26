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
