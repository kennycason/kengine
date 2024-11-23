package com.kengine.graphics

import com.kengine.context.Context

class TextureContext private constructor(
    val manager: TextureManager
) : Context() {

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