package com.kengine.sdl

//import sdl3.SDL_CreateRenderer
//import sdl3.SDL_CreateWindow
//import sdl3.SDL_DestroyRenderer
//import sdl3.SDL_DestroyWindow
//import sdl3.SDL_GetError
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
import sdl3.SDL_CreateRenderer
import sdl3.SDL_CreateWindow
import sdl3.SDL_GetError
import sdl3.SDL_INIT_VIDEO
import sdl3.SDL_Init
import sdl3.SDL_RenderClear
import sdl3.SDL_RenderPresent
import sdl3.SDL_SetRenderDrawColor
import sdl3.SDL_WINDOW_FULLSCREEN
import sdl3.SDL_WINDOW_RESIZABLE

//import sdl3.SDL_Quit
//import sdl3.SDL_RenderClear
//import sdl3.SDL_RenderPresent
//import sdl3.SDL_SetRenderDrawColor
//import sdl3.SDL_WINDOWPOS_CENTERED
//import sdl3.SDL_RENDERER_ACCELERATED
//
//import sdl3.image.IMG_INIT_JPG
//import sdl3.image.IMG_INIT_PNG
//import sdl3.image.IMG_Init
//import sdl3.image.IMG_Quit

@OptIn(ExperimentalForeignApi::class)
class SDL3Context private constructor(
    val screenWidth: Int,
    val screenHeight: Int,
    private val window: CValuesRef<cnames.structs.SDL_Window>,
    val renderer: CValuesRef<cnames.structs.SDL_Renderer>?,
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
//        SDL_DestroyRenderer(renderer)
//        SDL_DestroyWindow(window)
//        IMG_Quit()
//        SDL_Quit()
    }

    companion object {
        private val logger = Logger.get(SDL3Context::class)
        private var currentContext: SDL3Context? = null

        fun create(
            title: String,
            width: Int,
            height: Int,
            flags: ULong = SDL_WINDOW_FULLSCREEN or SDL_WINDOW_RESIZABLE
        ): SDL3Context {
            if (currentContext != null) {
                throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }

            if (!SDL_Init(SDL_INIT_VIDEO)) {
                logger.error("Error initializing SDL Video: ${SDL_GetError()?.toKString()}")
                exit(1)
            }
//            if (IMG_Init((IMG_INIT_PNG or IMG_INIT_JPG).toInt()) == 0) {
//                throw IllegalStateException("Failed to initialize SDL_image: ${SDL_GetError()?.toKString()}")
//            }

            val window = SDL_CreateWindow(
                title,
                width,
                height,
                flags
            ) ?: throw IllegalStateException("Error creating window: ${SDL_GetError()?.toKString()}")

            val renderer = SDL_CreateRenderer(window, null)
                ?: throw IllegalStateException("Error creating renderer: ${SDL_GetError()?.toKString()}")

            currentContext = SDL3Context(width, height, window, renderer)
            return currentContext!!
        }

        fun get(): SDL3Context {
            return currentContext ?: throw IllegalStateException("SDL3Context has not been created. Call create() first.")
        }
    }
}