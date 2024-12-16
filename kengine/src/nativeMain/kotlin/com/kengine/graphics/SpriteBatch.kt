package com.kengine.graphics

import cnames.structs.SDL_Texture
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import sdl2.SDL_Point
import sdl2.SDL_Rect
import sdl2.SDL_RenderCopy
import sdl2.SDL_RenderCopyEx

/**
 * NOTE SDL2 does not support batch.
 */
@OptIn(ExperimentalForeignApi::class)
class SpriteBatch : Logging {
    private val maxSprites = 1000

    // batch state
    private var drawing = false
    private var currentTexture: CValuesRef<SDL_Texture>? = null
    private var spriteCount = 0

    private data class BatchItem(
        val texture: CValuesRef<SDL_Texture>,
        val srcX: Int?,
        val srcY: Int?,
        val srcW: Int?,
        val srcH: Int?,
        val dstX: Int,
        val dstY: Int,
        val dstW: Int,
        val dstH: Int,
        val angle: Double = 0.0,
        val flip: FlipMode = FlipMode.NONE
    )

    private val batch = ArrayList<BatchItem>(maxSprites)

    fun begin() {
        if (drawing) throw IllegalStateException("SpriteBatch.end() must be called before begin()")
        drawing = true
        spriteCount = 0
        currentTexture = null
        batch.clear()
    }


    /**
     * Draw a portion of a texture (or entire if src parameters are null) to the screen.
     * If the current batch texture differs from the given texture, or we reached maxSprites,
     * we flush before adding the new item.
     */
    fun draw(
        texture: CValuesRef<SDL_Texture>,
        srcX: Int? = null,
        srcY: Int? = null,
        srcW: Int? = null,
        srcH: Int? = null,
        dstX: Int,
        dstY: Int,
        dstW: Int,
        dstH: Int,
        angle: Double = 0.0,
        flip: FlipMode = FlipMode.NONE
    ) {
        if (!drawing) throw IllegalStateException("SpriteBatch.begin() must be called before draw()")

        // If texture changes or we reached capacity, flush
        if (currentTexture != texture || spriteCount >= maxSprites) {
            flush()
            currentTexture = texture
        }

        batch.add(
            BatchItem(
                texture = texture,
                srcX = srcX, srcY = srcY,
                srcW = srcW, srcH = srcH,
                dstX = dstX, dstY = dstY,
                dstW = dstW, dstH = dstH,
                angle = angle,
                flip = flip
            )
        )
        spriteCount++
    }

    private fun flush() {
        if (spriteCount == 0) return

        if (logger.isDebugEnabled()) {
            logger.debug { "Flushing ${batch.size} sprites." }
        }

        useSDLContext {
            memScoped {
                for (item in batch) {
                    // allocate source rect if needed
                    val srect = if (item.srcX != null && item.srcY != null && item.srcW != null && item.srcH != null) {
                        alloc<SDL_Rect> {
                            x = item.srcX
                            y = item.srcY
                            w = item.srcW
                            h = item.srcH
                        }
                    } else null

                    // allocate destination rect
                    val drect = alloc<SDL_Rect> {
                        x = item.dstX
                        y = item.dstY
                        w = item.dstW
                        h = item.dstH
                    }

                    if (item.angle == 0.0 && item.flip == FlipMode.NONE) {
                        // no rotation/flipping needed
                        SDL_RenderCopy(
                            renderer,
                            item.texture,
                            srect?.ptr,
                            drect.ptr
                        )
                    } else {
                        // rotation/flip
                        val center = alloc<SDL_Point> {
                            x = drect.w / 2
                            y = drect.h / 2
                        }
                        SDL_RenderCopyEx(
                            renderer = renderer,
                            texture = item.texture,
                            srcrect = srect?.ptr,
                            dstrect = drect.ptr,
                            angle = item.angle,
                            center = center.ptr,
                            flip = item.flip.flag
                        )
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

    }
}