package com.kengine.graphics

import com.kengine.context.useContext
import com.kengine.sdl.SDLContext
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl2.SDL_Rect
import sdl2.SDL_RenderCopy

class Sprite {
    lateinit var texture: Texture
    val width: Int by lazy { texture.width }
    val height: Int by lazy { texture.height }

    /**
     * Create a sprite from an image file path.
     */
    constructor(imagePath: String) {
        useContext(TextureManagerContext.get()) {
            texture = textureManager.getTexture(imagePath)
        }
    }

    /**
     * Create a sprite directly from a texture.
     */
    constructor(texture: Texture) {
        this.texture = texture
    }

    fun draw(x: Double, y: Double) {
        val sdlContext = SDLContext.get()

        // define destination rectangle for rendering
        memScoped {
            val destRect = alloc<SDL_Rect>().apply {
                this.x = x.toInt()
                this.y = y.toInt()
                this.w = width
                this.h = height
            }
            SDL_RenderCopy(sdlContext.renderer, texture.texture, null, destRect.ptr)
        }
    }

    fun cleanup() {
        // TextureManager handles texture cleanup
    }

}