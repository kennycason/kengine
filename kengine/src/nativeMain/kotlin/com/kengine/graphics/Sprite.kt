package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.log.getLogger
import com.kengine.math.IntRect
import com.kengine.math.Math
import com.kengine.math.Vec2
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
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

    // pre-allocated SDL objects
    private val clipRect: SDL_FRect? = clip?.let {
        nativeHeap.alloc<SDL_FRect>().apply {
            this.x = it.x.toFloat() + 0.5f
            this.y = it.y.toFloat() + 0.5f
            this.w = it.w.toFloat() - 1f
            this.h = it.h.toFloat() - 1f
        }
    }
    private val destRect = nativeHeap.alloc<SDL_FRect>().apply {
        this.x = 0f
        this.y = 0f
        this.w = scaledWidth
        this.h = scaledHeight
    }
    private val origin = nativeHeap.alloc<SDL_FPoint>()
    private val right = nativeHeap.alloc<SDL_FPoint>()
    private val down = nativeHeap.alloc<SDL_FPoint>()

    fun draw(
        p: Vec2,
        flip: FlipMode = FlipMode.NONE,
        angle: Double = 0.0
    ) = draw(p.x, p.y, flip, angle)

    fun draw(
        x: Double,
        y: Double,
        flip: FlipMode = FlipMode.NONE,
        angle: Double = 0.0
    ) {
        useSDLContext {
            // handle no rotation or flipping early
            if (angle == 0.0 && flip == FlipMode.NONE) {
                destRect.apply {
                    this.x = x.toFloat()
                    this.y = y.toFloat()
                }

                if (!SDL_RenderTexture(renderer, texture.texture, clipRect?.ptr, destRect.ptr)) {
                    handleError()
                }
                return
            }

            // aply flipping
            val flipX = if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) -1f else 1f
            val flipY = if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) -1f else 1f

            // compute pivot point
            val pivotX = x + (scaledWidth / 2.0f)
            val pivotY = y + (scaledHeight / 2.0f)

            // calculate rotation with affine transform
            val rad = Math.toRadians(angle)
            val cos = kotlin.math.cos(rad).toFloat()
            val sin = kotlin.math.sin(rad).toFloat()

            val offsetX = (scaledWidth / 2.0f) * flipX
            val offsetY = (scaledHeight / 2.0f) * flipY

            // update affine points
            origin.apply {
                this.x = ((pivotX - offsetX * cos + offsetY * sin).toFloat())
                this.y = ((pivotY - offsetX * sin - offsetY * cos).toFloat())
            }
            right.apply {
                this.x = ((pivotX + offsetX * cos + offsetY * sin).toFloat())
                this.y = ((pivotY + offsetX * sin - offsetY * cos).toFloat())
            }
            down.apply {
                this.x = ((pivotX - offsetX * cos - offsetY * sin).toFloat())
                this.y = ((pivotY - offsetX * sin + offsetY * cos).toFloat())
            }

            // render with affine transform
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

    fun cleanup() {
        logger.info { "Cleaning up Sprite $this" }
        clipRect?.let { nativeHeap.free(it) }
        nativeHeap.free(destRect)
        nativeHeap.free(origin)
        nativeHeap.free(right)
        nativeHeap.free(down)
    }

    private fun handleError() {
        val error = SDL_GetError()?.toKString()
        logger.error("Error rendering sprite: $error")
        throw RuntimeException("Failed to render sprite: $error")
    }

    companion object {
        private val logger = getLogger(Sprite::class)

        // load sprite from file
        fun fromFilePath(filePath: String, clip: IntRect? = null): Sprite {
            val texture = getTextureContext().getTexture(filePath)
            logger.info { "Loaded texture: ${texture.width}x${texture.height}" }
            return Sprite(texture, clip)
        }

        // load sprite from existing texture
        fun fromTexture(texture: Texture, clip: IntRect? = null): Sprite {
            return Sprite(texture, clip)
        }

    }
}
