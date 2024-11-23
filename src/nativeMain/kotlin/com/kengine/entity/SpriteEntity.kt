package com.kengine.entity

import com.kengine.Vec2
import com.kengine.graphics.Sprite

open class SpriteEntity(
    protected val sprite: Sprite,
    override val p: Vec2 = Vec2(),
    override val v: Vec2 = Vec2(),
) : Entity {
    override val width: Int = sprite.width
    override val height: Int = sprite.height
    override fun update(elapsedSeconds: Double) {}
    override fun draw(elapsedSeconds: Double) {
        sprite.draw(p.x, p.y)
    }
    override fun cleanup() {
        sprite.cleanup()
    }
}
