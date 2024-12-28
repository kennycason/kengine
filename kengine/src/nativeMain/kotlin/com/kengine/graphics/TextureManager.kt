package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import sdl3.SDL_GetError
import sdl3.image.IMG_Load
import sdl3.image.SDL_CreateTexture
import sdl3.image.SDL_CreateTextureFromSurface
import sdl3.image.SDL_DestroySurface
import sdl3.image.SDL_DestroyTexture
import sdl3.image.SDL_RenderTexture
import sdl3.image.SDL_SetRenderTarget
import sdl3.image.SDL_Surface
import sdl3.image.SDL_Texture
import sdl3.image.SDL_TextureAccess

/**
 * A centralized texture manager to help with caching for faster, more efficient texture loading.
 */
@OptIn(ExperimentalForeignApi::class)
class TextureManager : Logging {
    private val textureCache = mutableMapOf<String, Texture>()

    fun getTexture(texturePath: String): Texture {
        useSDLContext {
            if (texturePath in textureCache) {
                logger.debug { "Loading texture $texturePath from cache" }
                return textureCache[texturePath]!!
            }
        }
        return addTexture(texturePath)
    }

    fun addTexture(texturePath: String): Texture {
        useSDLContext {
            logger.debug { "Loading texture $texturePath to cache" }

            // load the surface first
            val surface: CPointer<SDL_Surface> = IMG_Load(texturePath)
                ?: throw IllegalStateException("Error loading image: ${SDL_GetError()?.toKString()}")

            // extract width and height from the surface BEFORE destroying it
            val w = surface.pointed.w
            val h = surface.pointed.h
            val format = surface.pointed.format

            logger.info { "Loaded surface: width=$w, height=$h, format=$format" }

            // Cceate a texture from the surface
            val texture = SDL_CreateTextureFromSurface(renderer, surface)
                ?: throw IllegalStateException("Error creating texture from surface: ${SDL_GetError()?.toKString()}")

            // destroy the surface now that it's no longer needed
            SDL_DestroySurface(surface)

            // add to cache with extracted dimensions
            textureCache[texturePath] = Texture(
                texture = texture,
                width = w,
                height = h,
                format = format,
                access = SDL_TextureAccess.SDL_TEXTUREACCESS_STATIC.ordinal
            )

            logger.info { "Added texture to cache: path=$texturePath, width=$w, height=$h" }
        }

        // return the cached texture
        return textureCache[texturePath]!!
    }

    fun copyTexture(texturePath: String): Texture {
        val texture = getTexture(texturePath)
        return texture.copy(texture = copyTexture(texture.texture))
    }

    fun copyTexture(source: CPointer<SDL_Texture>): CPointer<SDL_Texture> {
        useSDLContext {
            // lookup dimensions from the cached texture, not source itself
            val cachedTexture = textureCache.values.find { it.texture == source }
                ?: throw IllegalStateException("Source texture not found in cache!")

            val targetTexture = SDL_CreateTexture(
                renderer,
                cachedTexture.format, // use cached format
                SDL_TextureAccess.SDL_TEXTUREACCESS_TARGET,
                cachedTexture.width,
                cachedTexture.height
            ) ?: throw IllegalStateException("Error creating new texture: ${SDL_GetError()?.toKString()}")

            // copy the source texture onto the new texture
            SDL_SetRenderTarget(renderer, targetTexture)
            SDL_RenderTexture(renderer, source, null, null)
            SDL_SetRenderTarget(renderer, null) // reset the render target back to default

            return targetTexture
        }
    }

    fun cleanup() {
        textureCache.forEach { (_, texture) ->
            SDL_DestroyTexture(texture.texture)
        }
        textureCache.clear()
    }
}
