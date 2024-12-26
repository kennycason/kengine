package com.kengine.graphics

import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.image.SDL_Texture

class TextureContext private constructor(
    private val manager: TextureManager
) : Context(), Logging {

    fun addTexture(texturePath: String) {
        manager.addTexture(texturePath)
    }

    fun getTexture(texturePath: String): Texture {
        return manager.getTexture(texturePath)
    }

    fun copyTexture(texturePath: String): Texture {
        return manager.copyTexture(texturePath)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun copyTexture(texture: CPointer<SDL_Texture>): CPointer<SDL_Texture> {
        return manager.copyTexture(texture)
    }

    companion object {
        private var currentContext: TextureContext? = null

        fun get(): TextureContext {
            if (currentContext == null) {
                currentContext = TextureContext(
                    manager = TextureManager()
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create TextureManagerContext")
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up TextureContext"}
        manager.cleanup()
        currentContext = null
    }
}
