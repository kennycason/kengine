package com.kengine.graphics

import com.kengine.hooks.context.getContext
import com.kengine.log.Logging
import com.kengine.math.IntRect

class SpriteSheet private constructor(
    private val texture: Texture,
    val tileWidth: Int,
    val tileHeight: Int,
    val marginX: Int = 0, // Margin around the tileset (horizontal)
    val marginY: Int = 0, // Margin around the tileset (vertical)
    val spacingX: Int = 0, // Spacing between tiles (horizontal)
    val spacingY: Int = 0  // Spacing between tiles (vertical)
) : Logging {
    val columns = (texture.width - marginX + spacingX) / (tileWidth + spacingX)
    val rows = (texture.height - marginY + spacingY) / (tileHeight + spacingY)

    val width: Int = columns * tileWidth + (columns - 1) * spacingX + marginX * 2
    val height: Int = rows * tileHeight + (rows - 1) * spacingY + marginY * 2

    init {
        logger.infoStream {
            write("SpriteSheet initialized: ")
            write("tile size ${tileWidth}x$tileHeight, ")
            write("margin ($marginX,$marginY), ")
            write("spacing ($spacingX,$spacingY)")
        }
    }

    /**
     * Get a specific tile as a ClippedSprite based on its grid position (x, y).
     */
    fun getTile(x: Int, y: Int): Sprite {
        val clip = IntRect(
            x = marginX + x * (tileWidth + spacingX),
            y = marginY + y * (tileHeight + spacingY),
            w = tileWidth,
            h = tileHeight
        )
        logger.debug { "Loading tile from ($x,$y) -> [(${clip.x},${clip.y}) ${clip.w}${clip.h}" }
        return Sprite.fromTexture(texture, clip)
    }

    fun cleanup() {
    }

    companion object {
        fun fromFilePath(
            filePath: String,
            tileWidth: Int,
            tileHeight: Int,
            offsetX: Int = 0,
            offsetY: Int = 0,
            paddingX: Int = 0,
            paddingY: Int = 0
        ): SpriteSheet {
            val texture = getContext<TextureContext>().getTexture(filePath)
            return SpriteSheet(texture, tileWidth, tileHeight, offsetX, offsetY, paddingX, paddingY)
        }

        fun fromTexture(
            texture: Texture,
            tileWidth: Int,
            tileHeight: Int,
            offsetX: Int = 0,
            offsetY: Int = 0,
            paddingX: Int = 0,
            paddingY: Int = 0
        ): SpriteSheet {
            return SpriteSheet(texture, tileWidth, tileHeight, offsetX, offsetY, paddingX, paddingY)
        }

        fun fromSprite(
            sprite: Sprite,
            tileWidth: Int,
            tileHeight: Int,
            offsetX: Int = 0,
            offsetY: Int = 0,
            paddingX: Int = 0,
            paddingY: Int = 0
        ): SpriteSheet {
            return SpriteSheet(sprite.texture, tileWidth, tileHeight, offsetX, offsetY, paddingX, paddingY)
        }
    }

}
