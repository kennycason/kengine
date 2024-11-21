package com.kengine.context

import kotlinx.cinterop.CValuesRef
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
import sdl2.SDL_WINDOWPOS_CENTERED
import sdl2.SDL_WINDOW_SHOWN

class SDLKontext private constructor(
    private val window: CValuesRef<cnames.structs.SDL_Window>,
    val renderer: CValuesRef<cnames.structs.SDL_Renderer>,
    val screenWidth: Int,
    val screenHeight: Int,
) : Kontext() {
    companion object {
        private var currentContext: SDLKontext? = null

        fun create(
            title: String,
            width: Int,
            height: Int,
            flags: UInt = SDL_WINDOW_SHOWN
        ): SDLKontext {
            if (currentContext != null) {
                throw IllegalStateException("SDLContext has already been created. Call cleanup() before creating a new context.")
            }

            // initialize SDL
            if (SDL_Init(SDL_INIT_VIDEO) != 0) {
                println("Error initializing SDL: ${SDL_GetError()?.toKString()}")
                exit(1)
            }

            // create the SDL window
            val window = SDL_CreateWindow(
                title,
                SDL_WINDOWPOS_CENTERED.toInt(),
                SDL_WINDOWPOS_CENTERED.toInt(),
                width,
                height,
                flags
            ) ?: throw IllegalStateException("Error creating window: ${SDL_GetError()?.toKString()}")

            // create the SDL renderer
            val renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED)
                ?: throw IllegalStateException("Error creating renderer: ${SDL_GetError()?.toKString()}")

            val context = SDLKontext(window, renderer, width, height)
            currentContext = context
            return context
        }

        fun get(): SDLKontext {
            return currentContext ?: throw IllegalStateException("SDLContext has not been created. Call create() first.")
        }
    }


    override fun cleanup() {
        SDL_DestroyRenderer(renderer)
        SDL_DestroyWindow(window)
        SDL_Quit()
        currentContext = null
    }
}