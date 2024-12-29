package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.math.IntRect
import com.kengine.math.Math
import com.kengine.sdl.getSDLContext
import com.kengine.sdl.image.copySdlVertex
import com.kengine.sdl.image.sdlFColor
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.free
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import sdl3.SDL_GetError
import sdl3.image.SDL_BLENDMODE_BLEND
import sdl3.image.SDL_FColor
import sdl3.image.SDL_RenderGeometry
import sdl3.image.SDL_SetRenderDrawBlendMode
import sdl3.image.SDL_SetRenderTarget
import sdl3.image.SDL_SetTextureBlendMode
import sdl3.image.SDL_Vertex
import kotlin.math.cos
import kotlin.math.sin

/**
 * Still WIP, achieving avg 13.ms (76 FPS) with batch, non-batch is 7ms
 */
@OptIn(ExperimentalForeignApi::class)
class SpriteBatch(
    val texture: Texture,
    initialCapacity: Int = 1024
) : Logging {
    private val defaultColor = sdlFColor(1f, 1f, 1f, 1f)  // Default white color
    private val renderer = getSDLContext().renderer

    // rre-allocated buffers
    private var capacity = initialCapacity
    private var vertexBuffer = nativeHeap.allocArray<SDL_Vertex>(capacity * 4)  // 4 vertices per sprite
    private var indexBuffer = nativeHeap.allocArray<IntVar>(capacity * 6)       // 6 indices per sprite

    // batch state
    private var vertexCount = 0
    private var indexCount = 0
    private var isBatching = false

    fun begin() {
        if (isBatching) {
            logger.warn { "Batch already started for texture $texture" }
            return
        }

        // enable alpha blending and set default render target
        SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND)
        SDL_SetRenderTarget(renderer, null) // reset target to default framebuffer
        SDL_SetTextureBlendMode(texture.texture, SDL_BLENDMODE_BLEND) // fix blending for textures
        isBatching = true
    }

    fun end() {
        if (!isBatching) {
            logger.warn { "Batch not started for texture $texture" }
            return
        }
        flush() // Ensure all queued vertices are rendered
        isBatching = false
    }

    fun draw(
        sprite: Sprite,
        x: Float,
        y: Float,
        flip: FlipMode = FlipMode.NONE,
        angle: Double = 0.0
    ) {
        // ensure blending is active
        SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND)

        if (!isBatching) {
            logger.warn { "Drawing without active batch for texture $texture" }
            return
        }

        // flush batch if sprite uses a different texture
        if (sprite.texture != texture) {
            logger.warn { "Sprite texture doesn't match batch texture. Flushing." }
            flush()
        }

        // grow buffers dynamically if either vertices or indices will overflow
        if (vertexCount + 4 > capacity * 4 || indexCount + 6 > capacity * 6) {
            grow()
        }
        val w = sprite.scaledWidth
        val h = sprite.scaledHeight

        // calculate UV coordinates
        val clip = sprite.clip ?: IntRect(0, 0, sprite.width, sprite.height)
        val texWidth = sprite.texture.width.toFloat()
        val texHeight = sprite.texture.height.toFloat()

        var (u0, u1) = clip.x / texWidth to (clip.x + clip.w) / texWidth
        var (v0, v1) = clip.y / texHeight to (clip.y + clip.h) / texHeight

        // flip UVs
        if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) {
            u0 = clip.x / texWidth
            u1 = (clip.x + clip.w) / texWidth
            val tmp = u0
            u0 = u1
            u1 = tmp
        }

        if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) {
            v0 = clip.y / texHeight
            v1 = (clip.y + clip.h) / texHeight
            val tmp = v0
            v0 = v1
            v1 = tmp
        }

        // debug UV bounds

        // calculate vertices with rotation
        val pivotX = x + w / 2.0f
        val pivotY = y + h / 2.0f

        val rad = Math.toRadians(angle)
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        fun rotate(px: Float, py: Float): Pair<Float, Float> {
            val tx = px - pivotX
            val ty = py - pivotY
            return (cosA * tx - sinA * ty + pivotX) to (sinA * tx + cosA * ty + pivotY)
        }

        // vertex positions
        val (x0, y0) = rotate(x, y)
        val (x1, y1) = rotate(x + w, y)
        val (x2, y2) = rotate(x + w, y + h)
        val (x3, y3) = rotate(x, y + h)

//        logger.debug {
//            "UVs after flipping: u0=$u0, u1=$u1, v0=$v0, v1=$v1 " +
//                "Clip: ${clip.x},${clip.y},${clip.w},${clip.h} " +
//                "Tex Size: ${sprite.texture.width}x${sprite.texture.height}"
//        }

        // add vertices to buffer
        addVertex(vertexCount + 0, x0, y0, defaultColor, u0, v0)
        addVertex(vertexCount + 1, x1, y1, defaultColor, u1, v0)
        addVertex(vertexCount + 2, x2, y2, defaultColor, u1, v1)
        addVertex(vertexCount + 3, x3, y3, defaultColor, u0, v1)

        // add indices
        val baseVertex = vertexCount
        indexBuffer[indexCount + 0] = baseVertex + 0
        indexBuffer[indexCount + 1] = baseVertex + 1
        indexBuffer[indexCount + 2] = baseVertex + 2
        indexBuffer[indexCount + 3] = baseVertex + 0
        indexBuffer[indexCount + 4] = baseVertex + 2
        indexBuffer[indexCount + 5] = baseVertex + 3

        vertexCount += 4
        indexCount += 6

        // frow buffers dynamically if either vertices or indices will overflow
        if (vertexCount + 4 > capacity * 4 || indexCount + 6 > capacity * 6) {
            flush() // flush before growing to avoid corrupted data
            grow()
        }
    }

    // add vertex to buffer
    private fun addVertex(index: Int, x: Float, y: Float, color: SDL_FColor, u: Float, v: Float) {
        val vertex = vertexBuffer[index]
        vertex.position.x = x
        vertex.position.y = y
        vertex.color.r = 1.0f
        vertex.color.g = 1.0f
        vertex.color.b = 1.0f
        vertex.color.a = 1.0f  // force opaque alpha
        vertex.tex_coord.x = u
        vertex.tex_coord.y = v
    }

    private fun grow() {
        val newCapacity = capacity * 2

        // allocate new buffers
        val newVertexBuffer = nativeHeap.allocArray<SDL_Vertex>(newCapacity * 4) // 4 vertices per sprite
        val newIndexBuffer = nativeHeap.allocArray<IntVar>(newCapacity * 6)     // 6 indices per sprite

        // copy existing vertex data
        for (i in 0 until vertexCount) {
            copySdlVertex(vertexBuffer[i], newVertexBuffer[i])
        }

        // copy existing index data
        for (i in 0 until indexCount) {
            newIndexBuffer[i] = indexBuffer[i]
        }

        // free old buffers
        nativeHeap.free(vertexBuffer)
        nativeHeap.free(indexBuffer)

        // update references
        vertexBuffer = newVertexBuffer
        indexBuffer = newIndexBuffer
        capacity = newCapacity

        logger.debug { "Grew batch buffers to capacity: $capacity" }
    }

    private fun flush() {
        if (vertexCount == 0) return

        // force blend mode and bind texture again
        SDL_SetTextureBlendMode(texture.texture, SDL_BLENDMODE_BLEND)
        SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND)
        SDL_SetRenderTarget(renderer, null)

        logger.debug { "Flushing batch: vertexCount=$vertexCount, indexCount=$indexCount" }

        if (!SDL_RenderGeometry(
                renderer,
                texture.texture,
                vertexBuffer,
                vertexCount,
                indexBuffer,
                indexCount
            )
        ) {
            val error = SDL_GetError()?.toKString()
            logger.error { "Failed to render batch: $error" }
            throw RuntimeException("Failed to render batch: $error")
        }

        // reset vertex/index counts
        vertexCount = 0
        indexCount = 0
    }

    fun cleanup() {
        nativeHeap.free(vertexBuffer)
        nativeHeap.free(indexBuffer)
    }
}
