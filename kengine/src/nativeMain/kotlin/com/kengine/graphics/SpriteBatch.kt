package com.kengine.graphics

import cnames.structs.SDL_Texture
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
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

    // pre-allocated native arrays to avoid per-sprite allocations
    private val srects = nativeHeap.allocArray<SDL_Rect>(maxSprites)
    private val drects = nativeHeap.allocArray<SDL_Rect>(maxSprites)
    private val centers = nativeHeap.allocArray<SDL_Point>(maxSprites)
    private val useSrc = BooleanArray(maxSprites)

    fun begin() {
        if (drawing) throw IllegalStateException("SpriteBatch.end() must be called before begin()")
        drawing = true
        spriteCount = 0
        currentTexture = null
        batch.clear()
    }

    /**
     * draw a portion of a texture (or entire if src parameters are null) to the screen.
     * if the current batch texture differs from the given texture, or we reached maxSprites,
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

        // if texture changes or we reached capacity, flush
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

        useSDLContext {
            // fill pre-allocated arrays with sprite data
            for (i in 0 until spriteCount) {
                val item = batch[i]

                // destination rect always exists
                drects[i].reinterpret<SDL_Rect>().apply {
                    x = item.dstX
                    y = item.dstY
                    w = item.dstW
                    h = item.dstH
                }

                // source rect is optional
                if (item.srcX != null && item.srcY != null && item.srcW != null && item.srcH != null) {
                    srects[i].reinterpret<SDL_Rect>().apply {
                        x = item.srcX
                        y = item.srcY
                        w = item.srcW
                        h = item.srcH
                    }

                    useSrc[i] = true
                } else {
                    useSrc[i] = false
                }

                // center for rotation if needed
                if (item.angle != 0.0 || item.flip != FlipMode.NONE) {
                    centers[i].reinterpret<SDL_Rect>().apply {
                        x = drects[i].w / 2
                        y = drects[i].h / 2
                    }
                }
            }

            // now render all
            for (i in 0 until spriteCount) {
                val item = batch[i]

                if (item.angle == 0.0 && item.flip == FlipMode.NONE) {
                    // Nn rotation/flipping
                    SDL_RenderCopy(
                        renderer,
                        item.texture,
                        if (useSrc[i]) srects[i].ptr else null,
                        drects[i].ptr
                    )
                } else {
                    // rotation/flip
                    SDL_RenderCopyEx(
                        renderer = renderer,
                        texture = item.texture,
                        srcrect = if (useSrc[i]) srects[i].ptr else null,
                        dstrect = drects[i].ptr,
                        angle = item.angle,
                        center = centers[i].ptr,
                        flip = item.flip.flag
                    )
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