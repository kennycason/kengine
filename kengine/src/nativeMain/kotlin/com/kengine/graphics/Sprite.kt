package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.log.getLogger
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

@OptIn(ExperimentalForeignApi::class)
class Sprite private constructor(
    val texture: Texture,
    val clip: IntRect? = null,
    val scale: Vec2 = Vec2(1.0, 1.0),
) : Logging {
    val width: Int = clip?.w ?: texture.width
    val height: Int = clip?.h ?: texture.height

    fun draw(p: Vec2, flip: FlipMode = FlipMode.NONE, angle: Double = 0.0) =
        draw(p.x, p.y, flip, angle)

    fun draw(x: Double, y: Double, flip: FlipMode = FlipMode.NONE, angle: Double = 0.0) {
        useSDLContext {
            memScoped {
                // define clipping rectangle if needed
                val clipRect = clip?.let {
                    alloc<SDL_FRect>().apply {
                        this.x = it.x.toFloat()
                        this.y = it.y.toFloat()
                        this.w = it.w.toFloat()
                        this.h = it.h.toFloat()
                    }
                }

                // calculate the scaled size
                val scaledWidth = (clipRect?.w ?: texture.width.toFloat()) * scale.x.toFloat()
                val scaledHeight = (clipRect?.h ?: texture.height.toFloat()) * scale.y.toFloat()

                // optimized rendering and early exit for fast path (no rotation, no flipping)
                if (angle == 0.0 && flip == FlipMode.NONE) {
                    val destRect = alloc<SDL_FRect>().apply {
                        this.x = x.toFloat()
                        this.y = y.toFloat()
                        this.w = scaledWidth
                        this.h = scaledHeight
                    }

                    if (!SDL_RenderTexture(renderer, texture.texture, clipRect?.ptr, destRect.ptr)) {
                        logger.error("Error drawing sprite: ${SDL_GetError()?.toKString()}")
                    }
                    return
                }

                // calculate flipping factors
                val flipX = if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) -1f else 1f
                // val flipY = if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) -1f else 1f

                // convert angle to radians
                val rad = Math.toRadians(angle)
                val cos = kotlin.math.cos(rad).toFloat()
                val sin = kotlin.math.sin(rad).toFloat()

                // calculate half dimensions for center-point rotation
                val halfWidth = scaledWidth / 2.0f
                val halfHeight = scaledHeight / 2.0f

                // Calculate the center position
                val centerX = x.toFloat() + halfWidth
                val centerY = y.toFloat() + halfHeight

                // Calculate the three points needed for SDL_RenderTextureAffine

                // origin point (top-left)
                val origin = alloc<SDL_FPoint>().apply {
                    this.x = centerX - halfWidth * cos * flipX + halfHeight * sin
                    this.y = centerY - halfWidth * sin * flipX - halfHeight * cos
                }

                // right point (top-right, moving along the width)
                val right = alloc<SDL_FPoint>().apply {
                    this.x = centerX + halfWidth * cos * flipX + halfHeight * sin
                    this.y = centerY + halfWidth * sin * flipX - halfHeight * cos
                }

                // down point (bottom-left, moving along the height)
                val down = alloc<SDL_FPoint>().apply {
                    this.x = centerX - halfWidth * cos * flipX - halfHeight * sin
                    this.y = centerY - halfWidth * sin * flipX + halfHeight * cos
                }

                // render using affine transform
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

    fun cleanup() {
        // texture manager handles cleanup of Texture/Surfaces
    }

    companion object {
        private val logger = getLogger(Sprite::class)

        fun fromFilePath(filePath: String, clip: IntRect? = null): Sprite {
            val texture = getTextureContext().getTexture(filePath)
            logger.info { "Loaded texture: ${texture.width}x${texture.height}" }
            return Sprite(texture, clip)
        }

        fun fromTexture(texture: Texture, clip: IntRect? = null): Sprite {
            return Sprite(texture, clip)
        }
    }
}
