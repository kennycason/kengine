package com.kengine.graphics

import com.kengine.log.Logging
import com.kengine.sdl.useSDLContext
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import sdl2.SDL_CreateTexture
import sdl2.SDL_DestroyTexture
import sdl2.SDL_GetError
import sdl2.SDL_QueryTexture
import sdl2.SDL_RenderCopy
import sdl2.SDL_SetRenderTarget
import sdl2.image.IMG_Load
import sdl2.image.SDL_CreateTextureFromSurface
import sdl2.image.SDL_FreeSurface

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

            val surface = IMG_Load(texturePath)
                ?: throw IllegalStateException("Error loading image: ${SDL_GetError()?.toKString()}")
            val texture = SDL_CreateTextureFromSurface(renderer, surface)
                ?: throw IllegalStateException("Error creating texture from surface: ${SDL_GetError()?.toKString()}")
            SDL_FreeSurface(surface)

            memScoped {
                val w = alloc<IntVar>()
                val h = alloc<IntVar>()
                val format = alloc<UIntVar>()
                val access = alloc<IntVar>()

                if (SDL_QueryTexture(texture, format.ptr, access.ptr, w.ptr, h.ptr) != 0) {
                    throw IllegalStateException("Error querying texture: ${SDL_GetError()?.toKString()}")
                }

                textureCache[texturePath] = Texture(
                    texture = texture,
                    width = w.value,
                    height = h.value,
                    format = format.value,
                    access = access.value
                )
            }
        }
        return textureCache[texturePath]!!
    }

    fun copyTexture(texturePath: String): Texture {
        val texture = getTexture(texturePath)
        return texture.copy(texture = copyTexture(texture.texture))
    }

    fun copyTexture(source: CValuesRef<cnames.structs.SDL_Texture>): CValuesRef<cnames.structs.SDL_Texture> {
        useSDLContext {
            memScoped {
                val w = alloc<IntVar>()
                val h = alloc<IntVar>()
                val format = alloc<UIntVar>()
                val access = alloc<IntVar>()

                if (SDL_QueryTexture(source, format.ptr, access.ptr, w.ptr, h.ptr) != 0) {
                    throw IllegalStateException("Error querying texture: ${SDL_GetError()?.toKString()}")
                }

                // create a new texture with the same dimensions and format
                val targetTexture = SDL_CreateTexture(renderer, format.value.toUInt(), access.value, w.value, h.value)
                    ?: throw IllegalStateException("Error creating new texture: ${SDL_GetError()?.toKString()}")

                // copy source texture to the new texture
                SDL_SetRenderTarget(renderer, targetTexture)
                SDL_RenderCopy(renderer, source, null, null)
                SDL_SetRenderTarget(renderer, null) // Reset the render target back to default

                return targetTexture
            }
        }
    }

    fun cleanup() {
        textureCache.forEach { (_, texture) -> SDL_DestroyTexture(texture.texture) }
        textureCache.clear()
    }
}