package com.kengine.font

import com.kengine.context.Context
import com.kengine.log.Logger
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import sdl2.ttf.SDL_FreeSurface
import sdl2.ttf.SDL_GetError
import sdl2.ttf.SDL_Surface
import sdl2.ttf.TTF_Init
import sdl2.ttf.TTF_OpenFont
import sdl2.ttf.TTF_Quit

@OptIn(ExperimentalForeignApi::class)
class FontContext : Context() {
    private val fontCache = mutableMapOf<String, Font>()
    private val surfaceCache = mutableMapOf<String, CPointer<SDL_Surface>>()

    init {
        if (TTF_Init() != 0) {
            throw IllegalStateException("Failed to initialize SDL_ttf: ${SDL_GetError()?.toKString()}")
        }
    }

    fun addFont(fontName: String, fontFilePath: String, fontSize: Int): Font {
        val key = "$fontName:$fontSize"
        Logger.info { "adding font $key" }
        return fontCache.getOrPut(key) {
            Font(
                name = fontName,
                font = TTF_OpenFont(fontFilePath, fontSize)
                    ?: throw IllegalStateException("Failed to load font at $fontFilePath: ${SDL_GetError()?.toKString()}"),
                fontSize = fontSize
            )
        }
    }

    fun getFont(fontName: String, fontSize: Int): Font {
        val key = "$fontName:$fontSize"
        return fontCache.getOrElse(key) {
            throw IllegalStateException("Failed to get font $fontName:$fontSize")
        }
    }

    fun drawText(
        font: Font,
        text: String,
        x: Int,
        y: Int,
        r: UByte, g: UByte, b: UByte, a: UByte = 0xFFu,
    ) {
        font.drawText(text, x, y, r, g, b, a)
    }

    fun clearCache() {
        surfaceCache.values.forEach { SDL_FreeSurface(it) }
        surfaceCache.clear()
    }

    override fun cleanup() {
        clearCache()
        fontCache.values.forEach { it.cleanup() }
        fontCache.clear()
        TTF_Quit()
    }

    companion object {
        private var instance: FontContext? = null

        fun get(): FontContext {
            if (instance == null) {
                instance = FontContext()
            }
            return instance!!
        }
    }
}