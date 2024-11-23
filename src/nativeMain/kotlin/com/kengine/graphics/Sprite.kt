package com.kengine.graphics

import com.kengine.sdl.SDLContext
import com.kengine.sdl.SDL_LoadBMP
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.exit
import sdl2.SDL_CreateTextureFromSurface
import sdl2.SDL_DestroyTexture
import sdl2.SDL_FreeSurface
import sdl2.SDL_GetError
import sdl2.SDL_QueryTexture
import sdl2.SDL_Rect
import sdl2.SDL_RenderCopy

class Sprite {
    private var texture: CValuesRef<cnames.structs.SDL_Texture>? = null
    private var textureWidth: Int = 0
    private var textureHeight: Int = 0

    val width: Int
        get() = textureWidth

    val height: Int
        get() = textureHeight

    /**
     * Create a sprite from an image file path.
     */
    constructor(imagePath: String) {
        loadFromImage(imagePath)
    }

    /**
     * Create a sprite directly from a texture.
     */
    constructor(texture: CValuesRef<cnames.structs.SDL_Texture>) {
        this.texture = texture
        memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            val result = SDL_QueryTexture(texture, null, null, w.ptr, h.ptr)
            if (result != 0) {
                println("Error querying texture dimensions: ${SDL_GetError()?.toKString()}")
                SDLContext.get().cleanup()
                exit(1)
            }
            textureWidth = w.value
            textureHeight = h.value
        }
    }

    /**
     * Load a texture from an image path.
     */
    private fun loadFromImage(imagePath: String) {
        val sdlContext = SDLContext.get()

        // load surface
        val surface = SDL_LoadBMP(imagePath)
        if (surface == null) {
            println("Error loading image: ${SDL_GetError()?.toKString()}")
            sdlContext.cleanup()
            exit(1)
            return
        }

        // create texture from surface
        texture = SDL_CreateTextureFromSurface(sdlContext.renderer, surface)
        if (texture == null) {
            println("Error creating texture: ${SDL_GetError()?.toKString()}")
            SDL_FreeSurface(surface)
            sdlContext.cleanup()
            exit(1)
            return
        }

        textureWidth = surface.pointed.w
        textureHeight = surface.pointed.h
        SDL_FreeSurface(surface)
    }

    fun draw(x: Double, y: Double) {
        val sdlKontext = SDLContext.get()

        // define destination rectangle for rendering
        memScoped {
            val destRect = alloc<SDL_Rect>().apply {
                this.x = x.toInt()
                this.y = y.toInt()
                this.w = width
                this.h = height
            }
            SDL_RenderCopy(sdlKontext.renderer, texture, null, destRect.ptr)
        }
    }

    fun cleanup() {
        texture?.let {
            SDL_DestroyTexture(it)
            texture = null
        }
    }

}