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
    val scale: Vec2 = Vec2(1.0, 1.0)
) : Logging {

    val width: Int = clip?.w ?: texture.width
    val height: Int = clip?.h ?: texture.height

    // computed scaled width and height
    val scaledWidth: Float
        get() = (width * scale.x).toFloat()

    val scaledHeight: Float
        get() = (height * scale.y).toFloat()

    fun draw(
        position: Vec2,
        flip: FlipMode = FlipMode.NONE,
        angle: Double = 0.0
    ) = draw(position.x, position.y, flip, angle)

    fun draw(
        x: Double,
        y: Double,
        flip: FlipMode = FlipMode.NONE,
        angle: Double = 0.0
    ) {
        useSDLContext {
            memScoped {
                // Prepare the clipping rectangle
                val clipRect = clip?.let {
                    alloc<SDL_FRect>().apply {
                        // add boundary to prevent texture bleed when scaled
                        this.x = it.x.toFloat() + 0.5f
                        this.y = it.y.toFloat() + 0.5f
                        this.w = it.w.toFloat() - 1f
                        this.h = it.h.toFloat() - 1f
                    }
                }

                // Apply flipping
                val flipX = if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) -1f else 1f
                val flipY = if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) -1f else 1f

                // Compute pivot point
                val pivotX = x + (scaledWidth / 2.0f)
                val pivotY = y + (scaledHeight / 2.0f)

                // Handle no rotation or flipping early
                if (angle == 0.0 && flip == FlipMode.NONE) {
                    val destRect = alloc<SDL_FRect>().apply {
                        this.x = x.toFloat()
                        this.y = y.toFloat()
                        this.w = scaledWidth
                        this.h = scaledHeight
                    }

                    if (!SDL_RenderTexture(renderer, texture.texture, clipRect?.ptr, destRect.ptr)) {
                        handleError()
                    }
                    return
                }

                // Calculate rotation with affine transform
                val rad = Math.toRadians(angle)
                val cos = kotlin.math.cos(rad).toFloat()
                val sin = kotlin.math.sin(rad).toFloat()

                val offsetX = (scaledWidth / 2.0f) * flipX
                val offsetY = (scaledHeight / 2.0f) * flipY

                // Affine points
                val origin = alloc<SDL_FPoint>().apply {
                    this.x = ((pivotX - offsetX * cos + offsetY * sin).toFloat())
                    this.y = ((pivotY - offsetX * sin - offsetY * cos).toFloat())
                }
                val right = alloc<SDL_FPoint>().apply {
                    this.x = ((pivotX + offsetX * cos + offsetY * sin).toFloat())
                    this.y = ((pivotY + offsetX * sin - offsetY * cos).toFloat())
                }
                val down = alloc<SDL_FPoint>().apply {
                    this.x = ((pivotX - offsetX * cos - offsetY * sin).toFloat())
                    this.y = ((pivotY - offsetX * sin + offsetY * cos).toFloat())
                }

                // Render with affine transform
                if (!SDL_RenderTextureAffine(
                    renderer,
                    texture.texture,
                    clipRect?.ptr,
                    origin.ptr,
                    right.ptr,
                    down.ptr
                )) {
                    handleError()
                }
            }
        }
    }

    fun cleanup() {
        //    texture.cleanup()
    }

    private fun handleError() {
        val error = SDL_GetError()?.toKString()
        logger.error("Error rendering sprite: $error")
        throw RuntimeException("Failed to render sprite: $error")
    }

    companion object {
        private val logger = getLogger(Sprite::class)

        // Load sprite from file
        fun fromFilePath(filePath: String, clip: IntRect? = null): Sprite {
            val texture = getTextureContext().getTexture(filePath)
            logger.info { "Loaded texture: ${texture.width}x${texture.height}" }
            return Sprite(texture, clip)
        }

        // Load sprite from existing texture
        fun fromTexture(texture: Texture, clip: IntRect? = null): Sprite {
            return Sprite(texture, clip)
        }
    }
}
