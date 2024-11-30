package com.kengine.sdl

import com.kengine.context.Context
import com.kengine.graphics.alphaFromRGBA
import com.kengine.graphics.blueFromRGBA
import com.kengine.graphics.greenFromRGBA
import com.kengine.graphics.redFromRGBA
import com.kengine.log.Logger
import com.kengine.log.Logging
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.exit
import sdl2.SDL_CreateRenderer
import sdl2.SDL_CreateWindow
import sdl2.SDL_DestroyRenderer
import sdl2.SDL_DestroyWindow
import sdl2.SDL_GetError
import sdl2.SDL_INIT_VIDEO
import sdl2.SDL_Init
import sdl2.SDL_Quit
import sdl2.SDL_RENDERER_ACCELERATED
import sdl2.SDL_RenderClear
import sdl2.SDL_RenderPresent
import sdl2.SDL_SetRenderDrawColor
import sdl2.SDL_WINDOWPOS_CENTERED
import sdl2.SDL_WINDOW_SHOWN

@OptIn(ExperimentalForeignApi::class)
class SDLContext private constructor(
    val screenWidth: Int,
    val screenHeight: Int,
    private val window: CValuesRef<cnames.structs.SDL_Window>,
    val renderer: CValuesRef<cnames.structs.SDL_Renderer>,
    val sdlEvents: SDLEventContext
) : Context(), Logging {

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
            flags: UInt = SDL_WINDOW_SHOWN
        ): SDLContext {
            if (currentContext != null) {
                throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }

            // init SDL
            if (SDL_Init(SDL_INIT_VIDEO) != 0) {
                logger.error("Error initializing SDL Video: ${SDL_GetError()?.toKString()}")
                exit(1)
            }

            // create window + renderer
            val window = SDL_CreateWindow(
                title,
                SDL_WINDOWPOS_CENTERED.toInt(),
                SDL_WINDOWPOS_CENTERED.toInt(),
                width,
                height,
                flags
            ) ?: throw IllegalStateException("Error creating window: ${SDL_GetError()?.toKString()}")

            val renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED)
                ?: throw IllegalStateException("Error creating renderer: ${SDL_GetError()?.toKString()}")


            currentContext = SDLContext(width, height, window, renderer, SDLEventContext.get())
            return currentContext!!
        }

        fun get(): SDLContext {
            return currentContext ?: throw IllegalStateException("SDLContext has not been created. Call create() first.")
        }
    }
}