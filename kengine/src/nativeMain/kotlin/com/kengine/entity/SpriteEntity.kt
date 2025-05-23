package com.kengine.entity

import com.kengine.graphics.FlipMode
import com.kengine.graphics.Sprite
import com.kengine.math.IntRect
import com.kengine.math.Vec2

open class SpriteEntity(
    protected val sprite: Sprite,
    var flipMode: FlipMode = FlipMode.NONE,
    var angle: Double = 0.0,
    p: Vec2 = Vec2(),
    v: Vec2 = Vec2(),
    a: Vec2 = Vec2()
) : Entity(
    p = p,
    v = v,
    a = a,
    width = sprite.width,
    height = sprite.height
) {
    override fun update() {}
    override fun draw() {
        sprite.draw(p.x, p.y, flipMode, angle)
    }

    override fun cleanup() {
       // sprite.cleanup()
    }

    companion object {
        fun fromFilePath(
            filePath: String,
            clip: IntRect? = null,
            flipMode: FlipMode = FlipMode.NONE,
            angle: Double = 0.0,
        ): SpriteEntity {
            return SpriteEntity(
                sprite = Sprite.fromFilePath(filePath, clip),
                flipMode = flipMode,
                angle = angle
            )
        }
    }
}
