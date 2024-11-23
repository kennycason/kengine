
package com.kengine.graphics

import com.kengine.sdl.SDLContext
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl2.SDL_Rect
import sdl2.SDL_RenderCopy

class ClippedSprite(
    private val sprite: Sprite,
    private val clipX: Int,
    private val clipY: Int,
    private val clipWidth: Int,
    private val clipHeight: Int
) {
    val width: Int
        get() = clipWidth

    val height: Int
        get() = clipHeight

    fun draw(x: Double, y: Double) {
        val sdlContext = SDLContext.get()

        memScoped {
            val srcRect = alloc<SDL_Rect>().apply {
                this.x = clipX
                this.y = clipY
                this.w = clipWidth
                this.h = clipHeight
            }

            val destRect = alloc<SDL_Rect>().apply {
                this.x = x.toInt()
                this.y = y.toInt()
                this.w = clipWidth
                this.h = clipHeight
            }

            SDL_RenderCopy(sdlContext.renderer, sprite.texture.texture, srcRect.ptr, destRect.ptr)
        }
    }

    fun cleanup() {
        sprite.cleanup()
    }
}