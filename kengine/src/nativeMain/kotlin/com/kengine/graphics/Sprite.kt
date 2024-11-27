package com.kengine.graphics

import com.kengine.math.Vec2
import com.kengine.context.useContext
import com.kengine.sdl.SDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl2.SDL_Rect
import sdl2.SDL_RenderCopy

@OptIn(ExperimentalForeignApi::class)
class Sprite {
    lateinit var texture: Texture
    val width: Int by lazy { texture.width }
    val height: Int by lazy { texture.height }
    val scale = Vec2(1.0, 1.0)

    /**
     * Create a sprite from an image file path.
     */
    constructor(imagePath: String) {
        useContext(TextureContext.get()) {
            texture = manager.getTexture(imagePath)
        }
    }

    /**
     * Create a sprite directly from a texture.
     */
    constructor(texture: Texture) {
        this.texture = texture
    }

    fun draw(p: Vec2) = draw(p.x, p.y)

    fun draw(x: Double, y: Double) {
        useContext(SDLContext.get()) {
            // define destination rectangle for rendering
            memScoped {
                val destRect = alloc<SDL_Rect>().apply {
                    this.x = (x * scale.x).toInt()
                    this.y = (y * scale.y).toInt()
                    this.w = (width * scale.x).toInt()
                    this.h = (height * scale.y).toInt()
                }
                SDL_RenderCopy(renderer, texture.texture, null, destRect.ptr)
            }
        }
    }

    fun cleanup() {
        // TextureManager handles texture cleanup
    }

}