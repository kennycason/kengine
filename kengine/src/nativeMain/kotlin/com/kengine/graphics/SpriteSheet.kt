package com.kengine.graphics

import com.kengine.log.Logger
import com.kengine.math.IntRect

class SpriteSheet(
    private val texture: Texture,
    val tileWidth: Int,
    val tileHeight: Int,
    private val offsetX: Int = 0,
    private val offsetY: Int = 0,
    private val dx: Int = 0, // spacing between tiles
    private val dy: Int = 0  // spacing between tiles
) {

    /**
     * Get a specific tile as a ClippedSprite based on its grid position (x, y).
     */
    fun getTile(x: Int, y: Int): Sprite {
        val clip = IntRect(
            x = offsetX + x * (tileWidth + dx),
            y = offsetY + y * (tileHeight + dy),
            w = tileWidth,
            h = tileHeight
        )
        Logger.debug { "Loading tile from ($x,$y) -> [(${clip.x},${clip.y}) ${clip.w}${clip.h}" }
        return Sprite.fromTexture(texture, clip)
    }

    fun cleanup() {
    }

}
