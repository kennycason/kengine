package com.kengine.graphics

import com.kengine.context.SDLKontext
import com.kengine.sdl.SDL_LoadBMP
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
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

class Sprite(
    private val imagePath: String,
) {
    private var texture: CValuesRef<cnames.structs.SDL_Texture>? = null
    private var textureWidth: Int = 0
    private var textureHeight: Int = 0

    val width: Int
        get() = textureWidth

    val height: Int
        get() = textureHeight

    private fun load() {
        val sdlKontext = SDLKontext.get()

        // Load surface
        val surface = SDL_LoadBMP(imagePath)
        if (surface == null) {
            println("Error loading image: ${SDL_GetError()?.toKString()}")
            sdlKontext.cleanup()
            exit(1)
        }

        // Create texture from surface
        texture = SDL_CreateTextureFromSurface(sdlKontext.renderer, surface)
        if (texture == null) {
            println("Error creating texture: ${SDL_GetError()?.toKString()}")
            SDL_FreeSurface(surface)
            sdlKontext.cleanup()
            exit(1)
        }

        // query texture for width and height
        memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            SDL_QueryTexture(texture, null, null, w.ptr, h.ptr)
            textureWidth = w.value
            textureHeight = h.value
        }

        SDL_FreeSurface(surface)
    }

    fun draw(x: Double, y: Double) {
        val sdlKontext = SDLKontext.get()

        if (texture == null) {
            load()
        }

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