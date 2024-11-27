package com.kengine.entity

import com.kengine.graphics.Sprite

open class SpriteEntity(
    protected val sprite: Sprite,
) : Entity(
    width = sprite.width,
    height = sprite.height
) {
    override fun update() {}
    override fun draw() {
        sprite.draw(p.x, p.y)
    }
    override fun cleanup() {
        sprite.cleanup()
    }
}
