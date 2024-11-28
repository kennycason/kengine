package com.kengine.graphics

import cnames.structs.SDL_Texture
import com.kengine.context.Context
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi

class TextureContext private constructor(
    private val manager: TextureManager
) : Context() {

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
    fun copyTexture(texture: CValuesRef<SDL_Texture>): CValuesRef<SDL_Texture> {
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
        manager.cleanup()
    }
}