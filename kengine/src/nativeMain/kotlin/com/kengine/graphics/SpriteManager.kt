package com.kengine.graphics

/**
 * A centralized sprite manager to help with caching for faster, more efficient sprite loading.
 */
class SpriteManager {
    private val sprites = mutableMapOf<String, Sprite>()
    private val spriteSheets = mutableMapOf<String, SpriteSheet>()

    fun addSprite(name: String, sprite: Sprite) {
        sprites[name] = sprite
    }

    fun getSprite(name: String): Sprite {
        return sprites[name]
            ?: throw IllegalStateException("Sprite not found: $name")
    }

    fun addSpriteSheet(name: String, spriteSheet: SpriteSheet) {
        spriteSheets[name] = spriteSheet
    }

    fun getSpriteSheet(name: String): SpriteSheet {
        return spriteSheets[name]
            ?: throw IllegalStateException("SpriteSheet not found: $name")
    }

    fun cleanup() {
        sprites.values.forEach { it.cleanup() }
        spriteSheets.values.forEach { it.cleanup() }
    }
}
