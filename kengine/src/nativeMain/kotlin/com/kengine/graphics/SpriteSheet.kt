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
    val width = texture.width - (2 * marginX)
    val height = texture.height - (2 * marginY)
    val columns = (width + spacingX) / (tileWidth + spacingX)
    val rows = (height + spacingY) / (tileHeight + spacingY)

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
        val pixelX = marginX + x * (tileWidth + spacingX)
        val pixelY = marginY + y * (tileHeight + spacingY)

        logger.debug {
            "getTile($x,$y) -> pixel($pixelX,$pixelY), tileSize=${tileWidth}x${tileHeight}, " +
                    "textureSize=${texture.width}x${texture.height}, margin=($marginX,$marginY), spacing=($spacingX,$spacingY)"
        }

        // TODO cache these as Array<Array<Sprite>> on init.
        return Sprite.fromTexture(
            texture,
            IntRect(x = pixelX, y = pixelY, w = tileWidth, h = tileHeight)
        )
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
