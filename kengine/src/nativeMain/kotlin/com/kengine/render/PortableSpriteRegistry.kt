package com.kengine.render

import com.kengine.assets.PortableAssetCatalog
import com.kengine.graphics.Sprite
import com.kengine.graphics.getSpriteContext
import com.kengine.graphics.useSpriteContext

class PortableSpriteRegistry {
    private val spriteNamesById = mutableMapOf<Int, String>()
    private val spriteSheetNamesById = mutableMapOf<Int, String>()

    fun registerSprite(name: String, sprite: Sprite): PortableSpriteRegistry {
        useSpriteContext {
            addSprite(name, sprite)
        }
        spriteNamesById[RenderAssetId.sprite(name)] = name
        return this
    }

    fun registerSpriteFromFilePath(name: String, filePath: String): PortableSpriteRegistry {
        useSpriteContext {
            addSpriteFromFilePath(name, filePath)
        }
        spriteNamesById[RenderAssetId.sprite(name)] = name
        return this
    }

    fun registerSpriteSheetFromFilePath(
        name: String,
        filePath: String,
        tileWidth: Int,
        tileHeight: Int,
        offsetX: Int = 0,
        offsetY: Int = 0,
        dx: Int = 0,
        dy: Int = 0
    ): PortableSpriteRegistry {
        useSpriteContext {
            addSpriteSheetFromFilePath(name, filePath, tileWidth, tileHeight, offsetX, offsetY, dx, dy)
        }
        spriteSheetNamesById[RenderAssetId.sprite(name)] = name
        return this
    }

    fun registerAssetsFromFilePaths(
        assets: PortableAssetCatalog,
        resolvePath: (String) -> String = { it }
    ): PortableSpriteRegistry {
        assets.sprites.forEach { sprite ->
            registerSpriteFromFilePath(
                name = sprite.id,
                filePath = resolvePath(sprite.source)
            )
        }
        assets.spriteSheets.forEach { spriteSheet ->
            registerSpriteSheetFromFilePath(
                name = spriteSheet.id,
                filePath = resolvePath(spriteSheet.source),
                tileWidth = spriteSheet.tileWidth,
                tileHeight = spriteSheet.tileHeight
            )
        }
        return this
    }

    internal fun getSprite(spriteId: Int, frame: Int = 0): Sprite? {
        spriteNamesById[spriteId]?.let { spriteName ->
            return getSpriteContext().getSprite(spriteName)
        }

        spriteSheetNamesById[spriteId]?.let { spriteSheetName ->
            val spriteSheet = getSpriteContext().getSpriteSheet(spriteSheetName)
            val frameCount = spriteSheet.columns * spriteSheet.rows
            val safeFrame = if (frameCount <= 0) 0 else positiveModulo(frame, frameCount)
            return spriteSheet.getTile(safeFrame % spriteSheet.columns, safeFrame / spriteSheet.columns)
        }

        return null
    }

    private fun positiveModulo(value: Int, divisor: Int): Int {
        val remainder = value % divisor
        return if (remainder < 0) remainder + divisor else remainder
    }
}
