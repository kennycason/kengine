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
                // Define clipping rectangle if needed
                val clipRect = clip?.let {
                    alloc<SDL_FRect>().apply {
                        this.x = it.x.toFloat()
                        this.y = it.y.toFloat()
                        this.w = it.w.toFloat()
                        this.h = it.h.toFloat()
                    }
                }

                // Calculate scaled size
                val scaledWidth = (clipRect?.w ?: texture.width.toFloat()) * scale.x.toFloat()
                val scaledHeight = (clipRect?.h ?: texture.height.toFloat()) * scale.y.toFloat()

                // Snap to grid: Calculate tile-aligned position
                val tileSize = 32.0 // Example fixed tile size, replace with actual size
                val alignedX = x.toFloat()
                val alignedY = y.toFloat()

                // Early exit for no rotation or flipping
                if (angle == 0.0 && flip == FlipMode.NONE) {
                    val destRect = alloc<SDL_FRect>().apply {
                        // Use exact x, y positions without over-adjusting
                        this.x = alignedX
                        this.y = alignedY
                        this.w = scaledWidth
                        this.h = scaledHeight
                    }

                    if (!SDL_RenderTexture(renderer, texture.texture, clipRect?.ptr, destRect.ptr)) {
                        logger.error("Error drawing sprite: ${SDL_GetError()?.toKString()}")
                    }
                    return
                }

                // Handle rotation and flipping
                val rad = Math.toRadians(angle)
                val cos = kotlin.math.cos(rad).toFloat()
                val sin = kotlin.math.sin(rad).toFloat()

                val flipX = if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) -1f else 1f
                val flipY = if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) -1f else 1f

                // Calculate pivot (center) for rotation
                val pivotX = alignedX + (tileSize / 2.0f)
                val pivotY = alignedY + (tileSize / 2.0f)

                // Define affine points
                val origin = alloc<SDL_FPoint>().apply {
                    this.x = (pivotX - (scaledWidth / 2.0f) * cos + (scaledHeight / 2.0f) * sin).toFloat()
                    this.y = (pivotY - (scaledWidth / 2.0f) * sin - (scaledHeight / 2.0f) * cos).toFloat()
                }
                val right = alloc<SDL_FPoint>().apply {
                    this.x = (pivotX + (scaledWidth / 2.0f) * cos + (scaledHeight / 2.0f) * sin).toFloat()
                    this.y = (pivotY + (scaledWidth / 2.0f) * sin - (scaledHeight / 2.0f) * cos).toFloat()
                }
                val down = alloc<SDL_FPoint>().apply {
                    this.x = (pivotX - (scaledWidth / 2.0f) * cos - (scaledHeight / 2.0f) * sin).toFloat()
                    this.y = (pivotY - (scaledWidth / 2.0f) * sin + (scaledHeight / 2.0f) * cos).toFloat()
                }

                // Render using affine transform
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
        // Add cleanup logic if needed
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
