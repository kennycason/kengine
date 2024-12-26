package com.kengine.graphics

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class SpriteContext private constructor(
    private val manager: SpriteManager,
    val spriteBatch: SpriteBatch,
) : Context(), Logging {

    fun getSprite(name: String): Sprite {
        return manager.getSprite(name)
    }

    fun getSpriteSheet(name: String): SpriteSheet {
        return manager.getSpriteSheet(name)
    }

    fun addSpriteFromFilePath(name: String, filePath: String) {
        manager.addSprite(name, Sprite.fromFilePath(filePath))
    }

    fun addSpriteFromTexture(name: String, texture: Texture) {
        manager.addSprite(name, Sprite.fromTexture(texture))
    }

    fun addSpriteSheet(name: String, spriteSheet: SpriteSheet) {
        manager.addSpriteSheet(name, spriteSheet)
    }

    fun addSpriteSheetFromFilePath(
        name: String,
        filePath: String,
        tileWidth: Int,
        tileHeight: Int,
        offsetX: Int = 0,
        offsetY: Int = 0,
        dx: Int = 0, // spacing between tiles
        dy: Int = 0  // spacing between tiles
    ) {
        manager.addSpriteSheet(name, SpriteSheet.fromFilePath(filePath, tileWidth, tileHeight, offsetX, offsetY, dx, dy))
    }

    fun addSpriteSheetFromTexture(
        name: String,
        texture: Texture,
        tileWidth: Int,
        tileHeight: Int,
        offsetX: Int = 0,
        offsetY: Int = 0,
        dx: Int = 0, // spacing between tiles
        dy: Int = 0  // spacing between tiles
    ) {
        manager.addSpriteSheet(name, SpriteSheet.fromTexture(texture, tileWidth, tileHeight, offsetX, offsetY, dx, dy))
    }

    fun addSpriteSheetFromSprite(
        name: String,
        sprite: Sprite,
        tileWidth: Int,
        tileHeight: Int,
        offsetX: Int = 0,
        offsetY: Int = 0,
        dx: Int = 0, // spacing between tiles
        dy: Int = 0  // spacing between tiles
    ) {
        manager.addSpriteSheet(name, SpriteSheet.fromSprite(sprite, tileWidth, tileHeight, offsetX, offsetY, dx, dy))
    }

    companion object {
        private var currentContext: SpriteContext? = null

        fun get(): SpriteContext {
            return currentContext ?: SpriteContext(
                manager = SpriteManager(),
                spriteBatch = SpriteBatch()
            ).also {
                currentContext = it
            }
        }
    }

    override fun cleanup() {
        logger.info { "Cleaning up SpriteContext" }
        manager.cleanup()
        spriteBatch.cleanup()
        currentContext = null
    }
}
