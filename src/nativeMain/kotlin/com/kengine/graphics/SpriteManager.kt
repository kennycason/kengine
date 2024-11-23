package com.kengine.graphics

/**
 * A centralized sprite manager to help with caching for faster, more efficient sprite loading.
 */
class SpriteManager {
    private val sprites = mutableMapOf<String, Sprite>()
    private val spriteSheets = mutableMapOf<String, SpriteSheet>()

    fun setSprite(name: String, sprite: Sprite) {
        sprites[name] = sprite
    }

    fun getSprite(name: String): Sprite {
       return sprites[name]!!
    }

    fun setSpriteSheet(name: String, spriteSheet: SpriteSheet) {
        spriteSheets[name] = spriteSheet
    }

    fun getSpriteSheet(name: String): SpriteSheet {
        return spriteSheets[name]!!
    }

    fun cleanup() {
        sprites.values.forEach { it.cleanup() }
        spriteSheets.values.forEach { it.cleanup() }
    }
}