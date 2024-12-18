package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import sdl3.SDL_GetError
import sdl3.SDL_GetNumberProperty
import sdl3.image.IMG_Load
import sdl3.image.SDL_CreateTexture
import sdl3.image.SDL_CreateTextureFromSurface
import sdl3.image.SDL_DestroySurface
import sdl3.image.SDL_DestroyTexture
import sdl3.image.SDL_GetTextureProperties
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

            val surface: CPointer<SDL_Surface> = IMG_Load(texturePath)
                ?: throw IllegalStateException("Error loading image: ${SDL_GetError()?.toKString()}")
            val texture = SDL_CreateTextureFromSurface(renderer, surface)
                ?: throw IllegalStateException("Error creating texture from surface: ${SDL_GetError()?.toKString()}")
            SDL_DestroySurface(surface)

            memScoped {
                val propertiesId = SDL_GetTextureProperties(texture)
                if (propertiesId != 0u) {
                    val w = SDL_GetNumberProperty(propertiesId, "width", 0).toInt()
                    val h = SDL_GetNumberProperty(propertiesId, "height", 0).toInt()
                    val format = SDL_GetNumberProperty(propertiesId, "format", 0).toUInt()
                    val access = SDL_GetNumberProperty(propertiesId, "access", 0).toInt()

                    textureCache[texturePath] = Texture(
                        texture = texture,
                        width = w,
                        height = h,
                        format = format,
                        access = access
                    )
                } else {
                    throw IllegalStateException("Error getting texture properties: ${SDL_GetError()?.toKString()}")
                }
            }
        }
        return textureCache[texturePath]!!
    }

    fun copyTexture(texturePath: String): Texture {
        val texture = getTexture(texturePath)
        return texture.copy(texture = copyTexture(texture.texture))
    }

    fun copyTexture(source: CValuesRef<SDL_Texture>): CValuesRef<SDL_Texture> {
        useSDLContext {
            val propertiesId = SDL_GetTextureProperties(source)
            if (propertiesId == 0u) {
                throw IllegalStateException("Error getting texture properties: ${SDL_GetError()?.toKString()}")
            }

            val w = SDL_GetNumberProperty(propertiesId, "width", 0).toInt()
            val h = SDL_GetNumberProperty(propertiesId, "height", 0).toInt()
            val format = SDL_GetNumberProperty(propertiesId, "format", 0).toUInt()
            val access = SDL_GetNumberProperty(propertiesId, "access", 0).toInt()

            // create a new texture with the same dimensions and format
            val targetTexture = SDL_CreateTexture(
                renderer,
                format,
                SDL_TextureAccess.values()[access],
                w,
                h
            ) ?: throw IllegalStateException("Error creating new texture: ${SDL_GetError()?.toKString()}")

            // copy source texture to the new texture
            SDL_SetRenderTarget(renderer, targetTexture)
            SDL_RenderTexture(renderer, source, null, null)
            SDL_SetRenderTarget(renderer, null) // Reset the render target back to default

            return targetTexture
        }
    }
    fun cleanup() {
        textureCache.forEach { (_, texture) -> SDL_DestroyTexture(texture.texture) }
        textureCache.clear()
    }
}
