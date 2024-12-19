package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.math.IntRect
import com.kengine.math.Math
import com.kengine.math.Vec2
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import sdl3.image.SDL_FPoint
import sdl3.image.SDL_FRect
import sdl3.image.SDL_GetError
import sdl3.image.SDL_RenderTexture
import sdl3.image.SDL_RenderTextureAffine

/**
 * TODO cache rects/points
 */
@OptIn(ExperimentalForeignApi::class)
class Sprite private constructor(
    val texture: Texture,
    val clip: IntRect? = null,
    val scale: Vec2 = Vec2(1.0, 1.0),
) : Logging {
    val width: Int = clip?.w ?: texture.width
    val height: Int = clip?.h ?: texture.height

    fun draw(p: Vec2, flip: FlipMode = FlipMode.NONE) = draw(p.x, p.y, flip)

    fun draw(x: Double, y: Double, flip: FlipMode = FlipMode.NONE, angle: Double = 0.0) {
        useSDLContext {
            memScoped {
                val clipRect = if (clip == null) null
                else alloc<SDL_FRect>().apply {
                    this.x = clip.x.toFloat()
                    this.y = clip.y.toFloat()
                    this.w = clip.w.toFloat()
                    this.h = clip.h.toFloat()
                }

                if (angle == 0.0 && flip == FlipMode.NONE) {
                    val destRect = alloc<SDL_FRect>().apply {
                        this.x = (x * scale.x).toFloat()
                        this.y = (y * scale.y).toFloat()
                        this.w = ((clipRect?.w ?: texture.width.toFloat()) * scale.x).toFloat()
                        this.h = ((clipRect?.h ?: texture.height.toFloat()) * scale.y).toFloat()
                    }

                    if (!SDL_RenderTexture(renderer, texture.texture, clipRect?.ptr, destRect.ptr)) {
                        logger.error("Error drawing sprite: ${SDL_GetError()?.toKString()}")
                    }
                } else {
                    val width = (clipRect?.w ?: texture.width.toFloat()) * scale.x.toFloat()
                    val height = (clipRect?.h ?: texture.height.toFloat()) * scale.y.toFloat()

                    val rad = Math.toRadians(angle)
                    val cos = kotlin.math.cos(rad).toFloat()
                    val sin = kotlin.math.sin(rad).toFloat()

                    val flipX = if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) -1f else 1f
                    val flipY = if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) -1f else 1f

                    // calculate destination points
                    val origin = alloc<SDL_FPoint>().apply {
                        this.x = x.toFloat()
                        this.y = y.toFloat()
                    }
                    val right = alloc<SDL_FPoint>().apply {
                        this.x = origin.x + width * cos * flipX
                        this.y = origin.y + width * sin * flipX
                    }
                    val down = alloc<SDL_FPoint>().apply {
                        this.x = origin.x - height * sin * flipY
                        this.y = origin.y + height * cos * flipY
                    }

                    val result = SDL_RenderTextureAffine(
                        renderer,
                        texture.texture,
                        clipRect?.ptr,
                        origin.ptr,
                        right.ptr,
                        down.ptr
                    )
                    if (!result) {
                        logger.error("Error drawing sprite: ${SDL_GetError()?.toKString()}")
                    }
                }
            }
        }
    }

    fun cleanup() {
    }

    companion object {
        fun fromFilePath(filePath: String, clip: IntRect? = null): Sprite {
            val texture = getTextureContext().getTexture(filePath)
            return Sprite(texture, clip)
        }

        fun fromTexture(texture: Texture, clip: IntRect? = null): Sprite {
            return Sprite(texture, clip)
        }
    }
}
