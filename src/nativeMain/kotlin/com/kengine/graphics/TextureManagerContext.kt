package com.kengine.graphics

import com.kengine.context.Context

class TextureManagerContext private constructor(
    val textureManager: TextureManager
) : Context() {

    companion object {
        private var currentContext: TextureManagerContext? = null

        fun get(): TextureManagerContext {
            if (currentContext == null) {
                currentContext = TextureManagerContext(
                    textureManager = TextureManager()
                )
            }
            return currentContext ?: throw IllegalStateException("GameContext has not been created. Call create() first.")
        }
    }

    override fun cleanup() {
        textureManager.cleanup()
    }
}