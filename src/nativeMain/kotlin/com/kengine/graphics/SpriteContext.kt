package com.kengine.graphics

import com.kengine.context.Context

class SpriteContext private constructor(
    val manager: SpriteManager
) : Context() {

    companion object {
        private var currentContext: SpriteContext? = null

        fun get(): SpriteContext {
            if (currentContext == null) {
                currentContext = SpriteContext(
                    manager = SpriteManager()
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create SpriteManagerContext")
        }
    }

    override fun cleanup() {
        manager.cleanup()
    }
}