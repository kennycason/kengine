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
    private val spriteSheet = SpriteContext.get().manager.getSpriteSheet(Sprites.BOXXLE_SHEET)
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
                val delta = when {
                    keyboard.isLeftPressed() -> Vec2(-1.0, 0.0).also { face = Direction.LEFT }
                    keyboard.isRightPressed() -> Vec2(1.0, 0.0).also { face = Direction.RIGHT }
                    keyboard.isUpPressed() -> Vec2(0.0, -1.0).also { face = Direction.UP }
                    keyboard.isDownPressed() -> Vec2(0.0, 1.0).also { face = Direction.DOWN }
                    else -> null
                }
                delta?.let { tryMove(it, elapsedSeconds) }
            }
        }
    }

    private fun tryMove(delta: Vec2, elapsedSeconds: Double) {
        useContext(BoxxleContext.get()) {
            val newP = p + delta

            // is a brick blocking the player?
            if (level.tiles.getOrNull(newP.y.toInt())?.getOrNull(newP.x.toInt()) == Tiles.BRICK) return

            // is player pushing box
            for (box in level.boxes) {
                if (newP == box.p) {
                    if (canMoveBox(box.p + delta)) {
                        // move both the player and the box
                        isMoving = true
                        ActionsContext.get().moveTo(this@Player, newP, speed, onComplete = { isMoving = false })
                        ActionsContext.get().moveTo(box, box.p + delta, speed, onComplete = { box.afterPush() })
                        lastMovedMs = getCurrentTimestampMilliseconds()
                        return
                    }
                    return // player can't move if the box can't be pushed
                }
            }

            // if no box is being pushed, just move the player
            isMoving = true
            ActionsContext.get().moveTo(this@Player, newP, speed, onComplete = { isMoving = false })
            lastMovedMs = getCurrentTimestampMilliseconds()
        }
    }

    private fun canMoveBox(newP: Vec2): Boolean {
        useContext(BoxxleContext.get()) {
            // is new position is within bounds
            if (!isWithinBounds(newP)) return false

            // is there a brick at the new position
            if (level.tiles[newP.y.toInt()][newP.x.toInt()] == Tiles.BRICK) return false

            // is another box is already at the new position
            if (level.boxes.any { it.p == newP }) return false

            return true
        }
    }

    private fun isWithinBounds(p: Vec2): Boolean {
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

    override fun cleanup() {
        playerSpriteUp.cleanup()
        playerSpriteDown.cleanup()
        playerSpriteLeft.cleanup()
        playerSpriteRight.cleanup()
    }

}