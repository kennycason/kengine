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

    val scale = Vec2(1.0, 1.0)
    var rotation: Double = 0.0

    fun draw(p: Vec2, flip: FlipMode = FlipMode.NONE) = draw(p.x, p.y, flip)

    fun draw(x: Double, y: Double, flip: FlipMode = FlipMode.NONE) {
        useContext(SDLContext.get()) {
            memScoped {
                val srcRect = alloc<SDL_Rect>().apply {
                    this.x = clipX
                    this.y = clipY
                    this.w = clipWidth
                    this.h = clipHeight
                }
                val destRect = alloc<SDL_Rect>().apply {
                    this.x = (x * scale.x).toInt()
                    this.y = (y * scale.y).toInt()
                    this.w = (clipWidth * scale.x).toInt()
                    this.h = (clipHeight * scale.y).toInt()
                }
                if (rotation == 0.0 && flip == FlipMode.NONE) {
                    SDL_RenderCopy(renderer, sprite.texture.texture, srcRect.ptr, destRect.ptr)
                } else {
                    val center = alloc<SDL_Point>().apply {
                        this.x = destRect.w / 2
                        this.y = destRect.h / 2
                    }
                    val result = SDL_RenderCopyEx(
                        renderer = renderer,
                        texture = sprite.texture.texture,
                        srcrect = srcRect.ptr,
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
        sprite.cleanup()
    }

}