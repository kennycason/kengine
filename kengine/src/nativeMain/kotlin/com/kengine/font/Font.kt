package com.kengine.font

import com.kengine.context.useContext
import com.kengine.graphics.alphaFromRGBA
import com.kengine.graphics.blueFromRGBA
import com.kengine.graphics.greenFromRGBA
import com.kengine.graphics.redFromRGBA
import com.kengine.log.Logger
import com.kengine.sdl.SDLContext
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.toKString
import sdl2.ttf.SDL_Color
import sdl2.ttf.SDL_CreateTextureFromSurface
import sdl2.ttf.SDL_DestroyTexture
import sdl2.ttf.SDL_FreeSurface
import sdl2.ttf.SDL_GetError
import sdl2.ttf.SDL_Rect
import sdl2.ttf.SDL_RenderCopy
import sdl2.ttf.SDL_Surface
import sdl2.ttf.TTF_CloseFont
import sdl2.ttf.TTF_Font
import sdl2.ttf.TTF_RenderText_Solid

@OptIn(ExperimentalForeignApi::class)
// look at: colorToRGBA
class Font(
    val name: String,
    val font: CPointer<TTF_Font>,
    val fontSize: Int
) {
    private val surfaceCache = mutableMapOf<String, CPointer<SDL_Surface>>()

    fun drawText(
        text: String,
        x: Int,
        y: Int,
        rgba: UInt,
        caching: Boolean = false
    ) {
        return drawText(text, x, y,
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
                Logger.debug { "Storing text surface: [$text]" }
                renderTextToSurface(text, font, r, g, b, a)
            }
        } else {
            renderTextToSurface(text, font, r, g, b, a)
        }

        useContext<SDLContext> {
            memScoped {
                val dstRect = alloc<SDL_Rect>().apply {
                    this.x = x
                    this.y = y
                    this.w = surface.pointed.w
                    this.h = surface.pointed.h
                }

                val texture = SDL_CreateTextureFromSurface(renderer, surface)
                    ?: throw IllegalStateException("Failed to create texture: ${SDL_GetError()?.toKString()}")

                SDL_RenderCopy(renderer, texture, null, dstRect.ptr)
                SDL_DestroyTexture(texture)
            }
        }

        if (!caching) {
            SDL_FreeSurface(surface)
        }
    }

    private fun renderTextToSurface(
        text: String,
        font: CPointer<TTF_Font>,
        r: UByte, g: UByte, b: UByte, a: UByte
    ): CPointer<SDL_Surface> {
        val sdlColor = toSDLColor(r, g, b, a)
        return TTF_RenderText_Solid(font, text, sdlColor)
            ?: throw IllegalStateException("Failed to render text: ${SDL_GetError()?.toKString()}")
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
        surfaceCache.clear()
    }

    fun cleanup() {
        TTF_CloseFont(font)
    }
}