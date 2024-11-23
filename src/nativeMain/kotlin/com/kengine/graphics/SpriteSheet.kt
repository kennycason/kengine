package com.kengine.graphics

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import sdl2.SDL_Rect

class SpriteSheet(
    private val sprite: Sprite,
    private val tileWidth: Int,
    private val tileHeight: Int,
    private val offsetX: Int = 0,
    private val offsetY: Int = 0,
    private val dx: Int = 0, // spacing between tiles
    private val dy: Int = 0  // spacing between tiles
) {

    /**
     * Get a specific tile as a ClippedSprite based on its grid position (x, y).
     */
    fun getTile(x: Int, y: Int): ClippedSprite {
        memScoped {
            val clipRect = alloc<SDL_Rect>().apply {
                this.x = offsetX + x * (tileWidth + dx)
                this.y = offsetY + y * (tileHeight + dy)
                this.w = tileWidth
                this.h = tileHeight
            }
            return ClippedSprite(sprite,
                clipX = clipRect.x,
                clipY = clipRect.y,
                clipWidth = clipRect.w,
                clipHeight = clipRect.h
            )
        }
    }
}
