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
import sdl3.image.SDL_BLENDMODE_BLEND
import sdl3.image.SDL_FColor
import sdl3.image.SDL_GetError
import sdl3.image.SDL_RenderGeometry
import sdl3.image.SDL_SetRenderDrawBlendMode
import sdl3.image.SDL_SetRenderTarget
import sdl3.image.SDL_SetTextureBlendMode
import sdl3.image.SDL_Vertex
import kotlin.math.cos
import kotlin.math.sin

// Matrix stack for nested transformations
class TransformMatrix {
    var a: Float = 1f  // scale x
    var b: Float = 0f  // skew y
    var c: Float = 0f  // skew x
    var d: Float = 1f  // scale y
    var tx: Float = 0f // translate x
    var ty: Float = 0f // translate y

    fun setToIdentity() {
        a = 1f; b = 0f
        c = 0f; d = 1f
        tx = 0f; ty = 0f
    }

    fun multiply(other: TransformMatrix) {
        val a1 = a * other.a + b * other.c
        val b1 = a * other.b + b * other.d
        val c1 = c * other.a + d * other.c
        val d1 = c * other.b + d * other.d
        val tx1 = tx * other.a + ty * other.c + other.tx
        val ty1 = tx * other.b + ty * other.d + other.ty

        a = a1; b = b1; c = c1; d = d1
        tx = tx1; ty = ty1
    }

    fun translate(x: Float, y: Float) {
        tx += x
        ty += y
    }

    fun scale(sx: Float, sy: Float) {
        a *= sx; b *= sx
        c *= sy; d *= sy
    }

    fun rotate(angle: Float) {
        val cos = cos(angle)
        val sin = sin(angle)

        val a1 = a * cos - b * sin
        val b1 = a * sin + b * cos
        val c1 = c * cos - d * sin
        val d1 = c * sin + d * cos

        a = a1; b = b1
        c = c1; d = d1
    }

    fun transformPoint(x: Float, y: Float): Pair<Float, Float> {
        return (a * x + c * y + tx) to (b * x + d * y + ty)
    }
}

@OptIn(ExperimentalForeignApi::class)
class SpriteBatch(
    val texture: Texture,
    initialCapacity: Int = 1024
) : Logging {
    private val defaultColor = sdlFColor(1f, 1f, 1f, 1f).also {
        logger.debug { "Default color created: r=${it.r} g=${it.g} b=${it.b} a=${it.a}" }
    }
    private val renderer = getSDLContext().renderer

    // Pre-allocated buffers
    private var capacity = initialCapacity
    private var vertexBuffer = nativeHeap.allocArray<SDL_Vertex>(capacity * 4)
    private var indexBuffer = nativeHeap.allocArray<IntVar>(capacity * 6)

    // Transform stack
    private val transformStack = mutableListOf<TransformMatrix>()
    private var currentTransform = TransformMatrix()

    // Shader support
    private var currentShader: Shader? = null
    private var shaderUniforms = mutableMapOf<String, Any>()

    // Batch state
    private var vertexCount = 0
    private var indexCount = 0
    var isBatching = false

    fun pushMatrix() {
        val copy = TransformMatrix().apply {
            a = currentTransform.a; b = currentTransform.b
            c = currentTransform.c; d = currentTransform.d
            tx = currentTransform.tx; ty = currentTransform.ty
        }
        transformStack.add(copy)
    }

    fun popMatrix() {
        if (transformStack.isNotEmpty()) {
            currentTransform = transformStack.removeAt(transformStack.lastIndex)
        }
    }

    fun translate(x: Float, y: Float) {
        currentTransform.translate(x, y)
    }

    fun scale(sx: Float, sy: Float) {
        currentTransform.scale(sx, sy)
    }

    fun rotate(angle: Float) {
        currentTransform.rotate(Math.toRadians(angle.toDouble()).toFloat())
    }

    fun setShader(shader: Shader?) {
        if (currentShader != shader) {
            flush() // Flush before changing shaders
            currentShader = shader
        }
    }

    fun setUniform(name: String, value: Any) {
        shaderUniforms[name] = value
    }

    fun begin() {
        if (isBatching) {
            logger.warn { "Batch already started for texture $texture" }
            return
        }

        SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND)
        SDL_SetRenderTarget(renderer, null)
        SDL_SetTextureBlendMode(texture.texture, SDL_BLENDMODE_BLEND)

        currentTransform.setToIdentity()
        transformStack.clear()
        isBatching = true
    }

    fun draw(
        sprite: Sprite,
        x: Float,
        y: Float,
        color: SDL_FColor = defaultColor,
        flip: FlipMode = FlipMode.NONE,
        angle: Double = 0.0,
        scaleX: Float = 1f,
        scaleY: Float = 1f
    ) {
        if (!isBatching) {
            logger.warn { "Drawing without active batch for texture $texture" }
            return
        }

        if (sprite.texture != texture) {
            logger.warn { "Sprite texture doesn't match batch texture. Flushing." }
            flush()
        }

//        if (vertexCount < 20) {  // only log first few to avoid spam
//            logger.debug { "Drawing sprite at ($x, $y) with flip=$flip angle=$angle" }
//        }

        if (vertexCount + 4 > capacity * 4 || indexCount + 6 > capacity * 6) {
            grow()
        }

        val w = sprite.scaledWidth * scaleX
        val h = sprite.scaledHeight * scaleY

        // Calculate UV coordinates
        val clip = sprite.clip ?: IntRect(0, 0, sprite.width, sprite.height)
        val texWidth = sprite.texture.width.toFloat()
        val texHeight = sprite.texture.height.toFloat()

        var (u0, u1) = clip.x / texWidth to (clip.x + clip.w) / texWidth
        var (v0, v1) = clip.y / texHeight to (clip.y + clip.h) / texHeight

        // Handle flipping
        if (flip == FlipMode.HORIZONTAL || flip == FlipMode.BOTH) {
            val tmp = u0; u0 = u1; u1 = tmp
        }
        if (flip == FlipMode.VERTICAL || flip == FlipMode.BOTH) {
            val tmp = v0; v0 = v1; v1 = tmp
        }

        // Transform vertices
        val (x0, y0) = currentTransform.transformPoint(x, y)
        val (x1, y1) = currentTransform.transformPoint(x + w, y)
        val (x2, y2) = currentTransform.transformPoint(x + w, y + h)
        val (x3, y3) = currentTransform.transformPoint(x, y + h)

        // Add vertices
        addVertex(vertexCount + 0, x0, y0, color, u0, v0)
        addVertex(vertexCount + 1, x1, y1, color, u1, v0)
        addVertex(vertexCount + 2, x2, y2, color, u1, v1)
        addVertex(vertexCount + 3, x3, y3, color, u0, v1)

        // Add indices
        val baseVertex = vertexCount
        indexBuffer[indexCount + 0] = baseVertex + 0
        indexBuffer[indexCount + 1] = baseVertex + 1
        indexBuffer[indexCount + 2] = baseVertex + 2
        indexBuffer[indexCount + 3] = baseVertex + 0
        indexBuffer[indexCount + 4] = baseVertex + 2
        indexBuffer[indexCount + 5] = baseVertex + 3

        vertexCount += 4
        indexCount += 6

//        logger.debug {
//            "Drawing sprite: pos=($x,$y) size=($w,$h) clip=${clip} " +
//                "uvs=($u0,$v0)-($u1,$v1) tex=${sprite.texture.width}x${sprite.texture.height}"
//        }

        if (vertexCount + 4 > capacity * 4 || indexCount + 6 > capacity * 6) {
            flush()
            grow()
        }
    }

    private fun addVertex(index: Int, x: Float, y: Float, color: SDL_FColor, u: Float, v: Float) {
        val vertex = vertexBuffer[index]
        vertex.position.x = x
        vertex.position.y = y
        vertex.color.r = color.r
        vertex.color.g = color.g
        vertex.color.b = color.b
        vertex.color.a = color.a
        vertex.tex_coord.x = u
        vertex.tex_coord.y = v
      //  logger.debug { "Added vertex $index: pos=($x,$y) uv=($u,$v) color=(${color.r},${color.g},${color.b},${color.a})" }
    }

    fun flush() {
        if (vertexCount == 0) return

        logger.debug { "Flushing batch with $vertexCount vertices" }

        // force blend mode and bind texture again
        SDL_SetTextureBlendMode(texture.texture, SDL_BLENDMODE_BLEND)
        SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND)
        SDL_SetRenderTarget(renderer, null)

        if (!SDL_RenderGeometry(
                renderer,
                texture.texture,
                vertexBuffer,
                vertexCount,
                indexBuffer,
                indexCount
            )) {  // Check for non-zero (error) return value
            val error = SDL_GetError()?.toKString()
            logger.error { "Failed to render batch: $error" }
            throw RuntimeException("Failed to render batch: $error")
        }
        // reset vertex/index counts
        vertexCount = 0
        indexCount = 0
    }
    fun end() {
        if (!isBatching) {
            logger.warn { "Batch not started for texture $texture" }
            return
        }
        flush()
        isBatching = false
    }

    private fun grow(minCapacity: Int = 0) {
        val newCapacity = maxOf(capacity * 2, minCapacity)

        val newVertexBuffer = nativeHeap.allocArray<SDL_Vertex>(newCapacity * 4)
        val newIndexBuffer = nativeHeap.allocArray<IntVar>(newCapacity * 6)

        // Copy existing data
        for (i in 0 until vertexCount) {
            copySdlVertex(vertexBuffer[i], newVertexBuffer[i])
        }
        for (i in 0 until indexCount) {
            newIndexBuffer[i] = indexBuffer[i]
        }

        nativeHeap.free(vertexBuffer)
        nativeHeap.free(indexBuffer)

        vertexBuffer = newVertexBuffer
        indexBuffer = newIndexBuffer
        capacity = newCapacity

        logger.debug { "Grew batch buffers to capacity: $capacity" }
    }

    fun cleanup() {
        nativeHeap.free(vertexBuffer)
        nativeHeap.free(indexBuffer)
        nativeHeap.free(defaultColor)
        transformStack.clear()
        shaderUniforms.clear()
    }
}

// Shader class (basic implementation)
class Shader(private val programId: Int) {
    fun use() {
        // Bind shader program
        // SDL3_gpu or custom implementation needed
    }

    fun unuse() {
        // Unbind shader program
    }

    fun setUniform(name: String, value: Any) {
        // Set uniform values
        // Implementation depends on your shader system
    }
}
