package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.math.Math
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import sdl3.SDL_GetError
import sdl3.image.SDL_FPoint
import sdl3.image.SDL_FRect
import sdl3.image.SDL_RenderTextureAffine
import sdl3.image.SDL_Texture

@OptIn(ExperimentalForeignApi::class)
class SpriteBatch : Logging {
    private val maxSprites = 1000

    private var drawing = false
    private var currentTexture: CValuesRef<SDL_Texture>? = null
    private var spriteCount = 0

    private data class BatchItem(
        val texture: CValuesRef<SDL_Texture>,
        val srcX: Int?,
        val srcY: Int?,
        val srcW: Int?,
        val srcH: Int?,
        val origin: SDL_FPoint,
        val right: SDL_FPoint,
        val down: SDL_FPoint
    )

    private val batch = ArrayList<BatchItem>(maxSprites)

    fun begin() {
        if (drawing) throw IllegalStateException("SpriteBatch.end() must be called before begin()")
        drawing = true
        spriteCount = 0
        currentTexture = null
        batch.clear()
    }

    fun draw(
        texture: CValuesRef<SDL_Texture>,
        srcX: Int? = null,
        srcY: Int? = null,
        srcW: Int? = null,
        srcH: Int? = null,
        dstX: Double,
        dstY: Double,
        dstW: Double,
        dstH: Double,
        angle: Double = 0.0,
        flip: FlipMode = FlipMode.NONE
    ) {
        if (!drawing) throw IllegalStateException("SpriteBatch.begin() must be called before draw()")

        if (currentTexture != texture || spriteCount >= maxSprites) {
            flush()
            currentTexture = texture
        }

        val rad = Math.toRadians(angle)
        val cos = kotlin.math.cos(rad).toFloat()
        val sin = kotlin.math.sin(rad).toFloat()

        val flipX = if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) -1f else 1f
        val flipY = if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) -1f else 1f

        memScoped {
            val origin = alloc<SDL_FPoint>().apply {
                x = dstX.toFloat()
                y = dstY.toFloat()
            }
            val right = alloc<SDL_FPoint>().apply {
                x = origin.x + (dstW * cos * flipX).toFloat()
                y = origin.y + (dstW * sin * flipX).toFloat()
            }
            val down = alloc<SDL_FPoint>().apply {
                x = origin.x - (dstH * sin * flipY).toFloat()
                y = origin.y + (dstH * cos * flipY).toFloat()
            }

            batch.add(
                BatchItem(
                    texture,
                    srcX,
                    srcY,
                    srcW,
                    srcH,
                    origin,
                    right,
                    down
                )
            )
        }

        spriteCount++
    }

    private fun flush() {
        if (spriteCount == 0) return

        useSDLContext {
            memScoped {
                for (item in batch) {
                    val clipRect = if (item.srcX != null && item.srcY != null && item.srcW != null && item.srcH != null) {
                        alloc<SDL_FRect>().apply {
                            x = item.srcX.toFloat()
                            y = item.srcY.toFloat()
                            w = item.srcW.toFloat()
                            h = item.srcH.toFloat()
                        }.ptr
                    } else {
                        null
                    }

                    if (!SDL_RenderTextureAffine(
                            renderer,
                            item.texture,
                            clipRect,
                            item.origin.ptr,
                            item.right.ptr,
                            item.down.ptr
                        )
                    ) {
                        logger.error("Error rendering sprite batch: ${SDL_GetError()?.toKString()}")
                    }
                }
            }
        }

        batch.clear()
        spriteCount = 0
    }

    fun end() {
        if (!drawing) throw IllegalStateException("SpriteBatch.begin() must be called before end()")
        if (spriteCount > 0) {
            flush()
        }
        drawing = false
        currentTexture = null
    }

    fun cleanup() {
        batch.clear()
    }
}
