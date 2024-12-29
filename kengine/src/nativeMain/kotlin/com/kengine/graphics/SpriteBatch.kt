package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.math.IntRect
import com.kengine.math.Math
import com.kengine.math.Vec2
import com.kengine.sdl.getSDLContext
import com.kengine.sdl.image.copySdlVertex
import com.kengine.sdl.image.sdlFColor
import com.kengine.sdl.image.sdlVertex
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.free
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import sdl3.SDL_GetError
import sdl3.image.SDL_RenderGeometry
import sdl3.image.SDL_Vertex
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalForeignApi::class)
class SpriteBatch(
    val texture: Texture
) : Logging {
    private val defaultColor = sdlFColor(1f, 1f, 1f, 1f)
    private val renderer = getSDLContext().renderer

    private var vertexBuffer: CPointer<SDL_Vertex>? = null
    private var indexBuffer: CPointer<IntVar>? = null

    private val vertices = mutableListOf<SDL_Vertex>()
    private val indices = mutableListOf<Int>()
    private var indexOffset = 0
    private var isBatching = false

    fun begin() {
        if (isBatching) {
            logger.warn { "Batch already started for texture $texture" }
            return
        }
        isBatching = true
    }

    fun end() {
        if (!isBatching) {
            logger.warn { "Batch not started for texture $texture" }
            return
        }
        flush()
        isBatching = false
    }

    fun draw(
        sprite: Sprite,
        position: Vec2,
        flip: FlipMode = FlipMode.NONE,
        angle: Double = 0.0
    ) {
        if (!isBatching) {
            logger.warn { "Drawing without active batch for texture $texture" }
            return
        }

        if (sprite.texture != texture) {
            logger.warn { "Sprite texture doesn't match batch texture" }
            return
        }

        val x = position.x.toFloat()
        val y = position.y.toFloat()
        val w = sprite.scaledWidth
        val h = sprite.scaledHeight

        val clip = sprite.clip ?: IntRect(0, 0, sprite.width, sprite.height)
        val texWidth = sprite.texture.width.toFloat()
        val texHeight = sprite.texture.height.toFloat()

        var (u0, u1) = clip.x / texWidth to (clip.x + clip.w) / texWidth
        var (v0, v1) = clip.y / texHeight to (clip.y + clip.h) / texHeight

        if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) {
            u0 = u1.also { u1 = u0 }
        }
        if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) {
            v0 = v1.also { v1 = v0 }
        }

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

        val (x0, y0) = rotate(x, y)
        val (x1, y1) = rotate(x + w, y)
        val (x2, y2) = rotate(x + w, y + h)
        val (x3, y3) = rotate(x, y + h)

        vertices.addAll(listOf(
            sdlVertex(x0, y0, defaultColor, u0, v0),
            sdlVertex(x1, y1, defaultColor, u1, v0),
            sdlVertex(x2, y2, defaultColor, u1, v1),
            sdlVertex(x3, y3, defaultColor, u0, v1)
        ))

        indices.addAll(listOf(
            indexOffset, indexOffset + 1, indexOffset + 2,
            indexOffset, indexOffset + 2, indexOffset + 3
        ))
        indexOffset += 4

        // Auto-flush if we're approaching buffer limits
        if (vertices.size >= 1024) {
            flush()
        }
    }

    private fun flush() {
        if (vertices.isEmpty() || indices.isEmpty()) return

        logger.debug { "Flushing batch: ${vertices.size} vertices, ${indices.size} indices" }

        memScoped {
            if (vertexBuffer == null || vertices.size > indexOffset) {
                vertexBuffer?.let { nativeHeap.free(it) }
                indexBuffer?.let { nativeHeap.free(it) }
                vertexBuffer = nativeHeap.allocArray(vertices.size)
                indexBuffer = nativeHeap.allocArray(indices.size)
            }

            vertices.forEachIndexed { i, v -> copySdlVertex(v, vertexBuffer!![i]) }
            indices.forEachIndexed { i, idx -> indexBuffer!![i] = idx }

            if (!SDL_RenderGeometry(
                    renderer,
                    texture.texture,
                    vertexBuffer,
                    vertices.size,
                    indexBuffer,
                    indices.size
                )
            ) {
                val error = SDL_GetError()?.toKString()
                logger.error { "Failed to render batch: $error" }
                throw RuntimeException("Failed to render batch: $error")
            }
        }

        cleanup()
    }

    private fun cleanup() {
        vertices.clear()
        indices.clear()
        indexOffset = 0
    }

    fun destroy() {
        vertexBuffer?.let { nativeHeap.free(it) }
        indexBuffer?.let { nativeHeap.free(it) }
        vertexBuffer = null
        indexBuffer = null
    }
}
