package com.kengine.graphics

import com.kengine.context.getContext
import com.kengine.log.Logging
import com.kengine.math.IntRect
import com.kengine.math.Vec2
import com.kengine.sdl.useSDLContext
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

// TODO object pools or locally storing SDL_Rects so I don't have to keep allocating them
@OptIn(ExperimentalForeignApi::class)
class Sprite private constructor(
    val texture: Texture,
    private val clip: IntRect? = null,
    val scale: Vec2 = Vec2(1.0, 1.0),
    var rotation: Double = 0.0
) : Logging {
    val width: Int = clip?.w ?: texture.width
    val height: Int = clip?.h ?: texture.height

    fun draw(p: Vec2, flip: FlipMode = FlipMode.NONE) = draw(p.x, p.y, flip)

    fun draw(x: Double, y: Double, flip: FlipMode = FlipMode.NONE) {
        useSDLContext {
            memScoped {
                val clipRect = if (clip == null) null
                else alloc<SDL_Rect>().apply {
                    this.x = clip.x
                    this.y = clip.y
                    this.w = clip.w
                    this.h = clip.h
                }

                val destRect = alloc<SDL_Rect>().apply {
                    this.x = (x * scale.x).toInt()
                    this.y = (y * scale.y).toInt()
                    this.w = ((clipRect?.w ?: texture.width) * scale.x).toInt()
                    this.h = ((clipRect?.h ?: texture.height) * scale.y).toInt()
                }

                if (rotation == 0.0 && flip == FlipMode.NONE) {
                    SDL_RenderCopy(renderer, texture.texture, clipRect?.ptr, destRect.ptr)
                } else {
                    val center = alloc<SDL_Point>().apply {
                        this.x = destRect.w / 2
                        this.y = destRect.h / 2
                    }
                    val result = SDL_RenderCopyEx(
                        renderer = renderer,
                        texture = texture.texture,
                        srcrect = clipRect?.ptr,
                        dstrect = destRect.ptr,
                        angle = rotation,
                        center = center.ptr,
                        flip = flip.flag
                    )
                    if (result != 0) {
                        logger.error("Error drawing sprite: ${SDL_GetError()?.toKString()}")
                    }
                    return
                }
            }
        }
    }

    fun cleanup() {
        // TextureManager handles texture cleanup
    }

    companion object {
        fun fromFilePath(filePath: String, clip: IntRect? = null): Sprite {
            val texture = getContext<TextureContext>().getTexture(filePath)
            return Sprite(texture, clip)
        }

        fun fromTexture(texture: Texture, clip: IntRect? = null): Sprite {
            return Sprite(texture, clip)
        }
    }

}