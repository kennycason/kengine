package games.boxxle

import com.kengine.Vec2
import com.kengine.action.ActionsContext
import com.kengine.context.useContext
import com.kengine.entity.Entity
import com.kengine.graphics.SpriteContext
import com.kengine.input.KeyboardContext
import com.kengine.time.getCurrentTimestampMilliseconds

private enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

class Player(
    override val p: Vec2 = Vec2(),
    override val v: Vec2 = Vec2(),
    override val width: Int = 32,
    override val height: Int = 32,
    private var scale: Double = 1.0
) : Entity {
    private val spriteSheet = SpriteContext.get().manager.getSpriteSheet("boxxle")
    private val playerSpriteUp = spriteSheet.getTile(0, 1)
    private val playerSpriteDown = spriteSheet.getTile(1, 1)
    private val playerSpriteLeft = spriteSheet.getTile(2, 1)
    private val playerSpriteRight = spriteSheet.getTile(3, 1)
    private var face: Direction = Direction.DOWN
    private var lastMovedMs = 0L
    private var isMoving = false
    private var speed = 7.0

    init {
        setScale(scale)
    }

    override fun update(elapsedSeconds: Double) {
        useContext(KeyboardContext.get()) {
            if (!isMoving && getCurrentTimestampMilliseconds() - lastMovedMs > 300) {
                if (keyboard.isLeftPressed()) {
                    v.x = -speed * elapsedSeconds
                    face = Direction.LEFT
                    move(-1.0, 0.0)
                } else if (keyboard.isRightPressed()) {
                    v.x = speed * elapsedSeconds
                    face = Direction.RIGHT
                    move(1.0, 0.0)
                } else if (keyboard.isUpPressed()) {
                    v.y = -speed * elapsedSeconds
                    face = Direction.UP
                    move(0.0, -1.0)
                } else if (keyboard.isDownPressed()) {
                    v.y = speed * elapsedSeconds
                    face = Direction.DOWN
                    move(0.0, 1.0)
                }
            }
        }
    }

    private fun move(dx: Double, dy: Double) {
        useContext(BoxxleContext.get()) {
            if (p.x + dx < 0 || p.x + dx >= level.tiles[0].size ||
                p.y + dy < 0 || p.y + dy >= level.tiles.size) return // TODO read from level dims

            if (level.tiles[(p.y + dy).toInt()][(p.x + dx).toInt()] == Tiles.BRICK) {
                return
            }
        }

        lastMovedMs = getCurrentTimestampMilliseconds()
        ActionsContext.get()
            .moveTo(this@Player, Vec2(p.x + dx, p.y + dy), speed, onComplete = { isMoving = false })
        pushBoxIfExists(Vec2(dx, dy))
    }

    fun pushBoxIfExists(delta: Vec2): Boolean {
        useContext(BoxxleContext.get()) {
            val newP = p + delta

            for (box in level.boxes) {
                if (newP == box.p) {
                    if (canMoveBox(box.p + delta)) {
                        ActionsContext.get()
                            .moveTo(box, box.p + delta, speed, onComplete = { box.afterPush() })
                        return true
                    }
                    return false
                }
            }
            return true
        }
    }

    fun canMoveBox(newP: Vec2): Boolean {
        useContext(BoxxleContext.get()) {
            // check if the new position is within level boundaries and not colliding with walls or other boxes
            if (!isWithinBounds(newP)) {
                return false
            }

            if (level.tiles[p.y.toInt()][p.x.toInt()] == Tiles.BRICK) {
                return false
            }

            for (box in level.boxes) {
                if (box.p == newP) {
                    return false // box is blocking
                }
            }

            return true // box can move
        }
    }

    fun isWithinBounds(p: Vec2): Boolean {
        useContext(BoxxleContext.get()) {
            return p.x >= 0 && p.y >= 0 && p.x < level.tiles[0].size && p.y < level.tiles.size
        }
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

    fun setScale(scale: Double) {
        this.scale = scale
        playerSpriteUp.scale.set(scale)
        playerSpriteDown.scale.set(scale)
        playerSpriteLeft.scale.set(scale)
        playerSpriteRight.scale.set(scale)
    }

    fun getScale() = scale

    override fun cleanup() {
        playerSpriteUp.cleanup()
        playerSpriteDown.cleanup()
        playerSpriteLeft.cleanup()
        playerSpriteRight.cleanup()
    }

}