package com.kengine.font

import com.kengine.graphics.alphaFromRGBA
import com.kengine.graphics.blueFromRGBA
import com.kengine.graphics.greenFromRGBA
import com.kengine.graphics.redFromRGBA
import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.size_t
import sdl3.ttf.SDL_Color
import sdl3.ttf.SDL_CreateTextureFromSurface
import sdl3.ttf.SDL_DestroySurface
import sdl3.ttf.SDL_DestroyTexture
import sdl3.ttf.SDL_FRect
import sdl3.ttf.SDL_GetError
import sdl3.ttf.SDL_RenderTexture
import sdl3.ttf.SDL_Surface
import sdl3.ttf.TTF_CloseFont
import sdl3.ttf.TTF_GetStringSize
import sdl3.ttf.TTF_RenderText_Solid

@OptIn(ExperimentalForeignApi::class)
class Font(
    val name: String,
    val font: CPointer<cnames.structs.TTF_Font>,
    val fontSize: Float
) : Logging {
    private val surfaceCache = mutableMapOf<String, CPointer<SDL_Surface>>()

    /**
     * Measures the width of the rendered text in pixels.
     */
    fun measureTextWidth(text: String): Int {
        memScoped {
            val width = alloc<IntVar>()
            val height = alloc<IntVar>()
            val result = TTF_GetStringSize(
                font = font,
                text = text,
                length = text.length.convert<size_t>(),
                w = width.ptr,
                h = height.ptr
            )
            if (!result) {
                throw IllegalStateException("Failed to measure text size: ${SDL_GetError()?.toKString()}")
            }
            return width.value
        }
    }


    fun drawText(
        text: String,
        x: Int,
        y: Int,
        rgba: UInt,
        caching: Boolean = false
    ) {
        return drawText(
            text, x, y,
            r = redFromRGBA(rgba),
            g = greenFromRGBA(rgba),
            b = blueFromRGBA(rgba),
            a = alphaFromRGBA(rgba),
            caching
        )
    }

    fun drawText(
        text: String,
        x: Int,
        y: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu,
        caching: Boolean = false
    ) {
        val cacheKey = if (caching) "${text.hashCode()}:$name:$fontSize:$r:$g:$b:$a" else ""
        val surface = if (caching) {
            surfaceCache.getOrPut(cacheKey) {
                logger.debug { "Storing text surface: [$text]" }
                renderTextToSurface(text, font, r, g, b, a)
            }
        } else {
            renderTextToSurface(text, font, r, g, b, a)
        }

        useSDLContext {
            memScoped {
                val dstRect = alloc<SDL_FRect>().apply {
                    this.x = x.toFloat()
                    this.y = y.toFloat()
                    this.w = surface.pointed.w.toFloat()
                    this.h = surface.pointed.h.toFloat()
                }

                val texture = SDL_CreateTextureFromSurface(renderer, surface)
                    ?: throw IllegalStateException("Failed to create texture: ${SDL_GetError()?.toKString()}")

                SDL_RenderTexture(renderer, texture, null, dstRect.ptr)
                SDL_DestroyTexture(texture)
            }
        }

        if (!caching) {
            SDL_DestroySurface(surface)
        }
    }

    private fun renderTextToSurface(
        text: String,
        font: CPointer<cnames.structs.TTF_Font>,
        r: UByte, g: UByte, b: UByte, a: UByte
    ): CPointer<SDL_Surface> {
        val sdlColor = toSDLColor(r, g, b, a)
        return TTF_RenderText_Solid(
            font, text,
            fg = sdlColor,
            length = text.length.convert<size_t>()
        ) ?: throw IllegalStateException("Failed to render text: ${SDL_GetError()?.toKString()}")
    }

    private fun toSDLColor(r: UByte, g: UByte, b: UByte, a: UByte): CValue<SDL_Color> {
        return memScoped {
            alloc<SDL_Color>().apply {
                this.r = r
                this.g = g
                this.b = b
                this.a = a
            }.readValue()
        }
    }

    fun clearCache() {
        surfaceCache.values.forEach { SDL_DestroySurface(it) }
        surfaceCache.clear()
    }

    fun cleanup() {
        clearCache()
        TTF_CloseFont(font)
    }
}
