package com.kengine.font

import com.kengine.file.File
import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import sdl3.ttf.SDL_DestroySurface
import sdl3.ttf.SDL_GetError
import sdl3.ttf.SDL_Surface
import sdl3.ttf.TTF_Init
import sdl3.ttf.TTF_OpenFont
import sdl3.ttf.TTF_Quit

@OptIn(ExperimentalForeignApi::class)
class FontContext : Context(), Logging {
    private val fontCache = mutableMapOf<String, Font>()
    private val surfaceCache = mutableMapOf<String, CPointer<SDL_Surface>>()

    init {
        if (!TTF_Init()) {
            throw IllegalStateException("Failed to initialize SDL_ttf: ${SDL_GetError()?.toKString()}")
        }
    }

    fun addFont(fontName: String, fontFilePath: String, fontSize: Float): Font {
        val fullFontFilePath = File.pwd() + "/" + fontFilePath
        if (!File.isExist(fullFontFilePath)) {
            throw IllegalStateException("Font file does not exist at $fullFontFilePath")
        }

        val key = "$fontName:$fontSize"
        logger.info { "adding font $key@$fullFontFilePath" }
        try {
            if (fontCache.containsKey(key)) {
                throw IllegalStateException("Font $key already registered.")
            }
            fontCache[key] = Font(
                name = fontName,
                font = TTF_OpenFont(fullFontFilePath, fontSize)
                    ?: throw IllegalStateException("Failed to load font at $fullFontFilePath: ${SDL_GetError()?.toKString()}"),
                fontSize = fontSize
            )
            return fontCache.getValue(key)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load font at $fullFontFilePath: ${SDL_GetError()?.toKString()}")
        }
    }

    fun getFont(fontName: String, fontSize: Float): Font {
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
        surfaceCache.values.forEach { SDL_DestroySurface(it) }
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
