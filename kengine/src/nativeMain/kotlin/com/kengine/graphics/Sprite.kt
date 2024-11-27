package com.kengine.graphics

import com.kengine.context.useContext
import com.kengine.log.Logger
import com.kengine.math.Vec2
import com.kengine.sdl.SDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import sdl2.SDL_GetError
import sdl2.SDL_Point
import sdl2.SDL_Rect
import sdl2.SDL_RenderCopy
import sdl2.SDL_RenderCopyEx

@OptIn(ExperimentalForeignApi::class)
class Sprite {
    lateinit var texture: Texture
    val width: Int by lazy { texture.width }
    val height: Int by lazy { texture.height }
    val scale = Vec2(1.0, 1.0)
    var rotation: Double = 0.0

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

    fun draw(p: Vec2, flip: FlipMode = FlipMode.NONE) = draw(p.x, p.y, flip)

    fun draw(x: Double, y: Double, flip: FlipMode = FlipMode.NONE) {
        useContext(SDLContext.get()) {
            // define destination rectangle for rendering
            memScoped {
                val destRect = alloc<SDL_Rect>().apply {
                    this.x = (x * scale.x).toInt()
                    this.y = (y * scale.y).toInt()
                    this.w = (width * scale.x).toInt()
                    this.h = (height * scale.y).toInt()
                }
                if (rotation == 0.0 && flip == FlipMode.NONE) {
                    SDL_RenderCopy(renderer, texture.texture, null, destRect.ptr)
                } else {
                    val center = alloc<SDL_Point>().apply {
                        this.x = destRect.w / 2
                        this.y = destRect.h / 2
                    }
                    val result = SDL_RenderCopyEx(
                        renderer = renderer,
                        texture = texture.texture,
                        srcrect = null,
                        dstrect = destRect.ptr,
                        angle = rotation,
                        center = center.ptr,
                        flip = flip.flag
                    )
                    if (result != 0) {
                        Logger.error("Error drawing sprite: ${SDL_GetError()?.toKString()}")
                    }
                    return
                }
            }
        }
    }

    fun cleanup() {
        // TextureManager handles texture cleanup
    }

}