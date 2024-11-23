package games.boxxle

import com.kengine.Vec2
import com.kengine.context.useContext
import com.kengine.entity.Entity
import com.kengine.graphics.SpriteContext
import com.kengine.input.KeyboardContext

private enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

class Player(
    override val p: Vec2 = Vec2(),
    override val v: Vec2 = Vec2(),
    override val width: Int = 32,
    override val height: Int = 32,
    var scale: Double = 1.0,
): Entity {
    private val spriteSheet = SpriteContext.get().manager.getSpriteSheet("boxxle")
    private val playerSpriteUp = spriteSheet.getTile(0, 1)
    private val playerSpriteDown = spriteSheet.getTile(1, 1)
    private val playerSpriteLeft = spriteSheet.getTile(2, 1)
    private val playerSpriteRight = spriteSheet.getTile(3, 1)
    private var face: Direction = Direction.DOWN
    private var lastMovedMs = 0
    private var speed = 7.0

    init {
        playerSpriteUp.scale.set(scale)
        playerSpriteDown.scale.set(scale)
        playerSpriteLeft.scale.set(scale)
        playerSpriteRight.scale.set(scale)
    }

    override fun update(elapsedSeconds: Double) {
        v.x *= 0.9
        v.y *= 0.9
        useContext(KeyboardContext.get()) {
            if (keyboard.isLeftPressed()) {
                v.x = -speed * elapsedSeconds
                face = Direction.LEFT
            }
            if (keyboard.isRightPressed()) {
                v.x = speed * elapsedSeconds
                face = Direction.RIGHT
            }
            if (keyboard.isUpPressed()) {
                v.y = -speed * elapsedSeconds
                face = Direction.UP
            }
            if (keyboard.isDownPressed()) {
                v.y = speed * elapsedSeconds
                face = Direction.DOWN
            }
        }

        p.x += v.x
        p.y += v.y
    }

    override fun draw(elapsedSeconds: Double) {
        useContext(SpriteContext.get()) {
            when (face) {
                Direction.UP -> playerSpriteUp.draw(p.x * 32, p.y * 32)
                Direction.DOWN -> playerSpriteDown.draw(p.x * 32, p.y * 32)
                Direction.LEFT -> playerSpriteLeft.draw(p.x * 32, p.y * 32)
                Direction.RIGHT -> playerSpriteRight.draw(p.x * 32, p.y * 32)
            }
        }
    }

    override fun cleanup() {
        playerSpriteUp.cleanup()
        playerSpriteDown.cleanup()
        playerSpriteLeft.cleanup()
        playerSpriteRight.cleanup()
    }

}