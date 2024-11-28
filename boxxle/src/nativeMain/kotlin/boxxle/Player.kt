package boxxle

import com.kengine.GameContext
import com.kengine.action.ActionsContext
import com.kengine.context.getContext
import com.kengine.context.useContext
import com.kengine.entity.Entity
import com.kengine.graphics.SpriteContext
import com.kengine.input.KeyboardContext
import com.kengine.math.Vec2
import com.kengine.time.getCurrentTimestampMilliseconds
import com.kengine.time.timeSinceMs

private enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

class Player(
    p: Vec2,
    private var scale: Double = 1.0
) : Entity(
    p = p, width = 32, height = 32,
) {
    private val spriteSheet = SpriteContext.get().getSpriteSheet(Sprites.BOXXLE_SHEET)
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

    override fun update() {
        useContext<KeyboardContext> {
            if (!isMoving && timeSinceMs(lastMovedMs) > 300) {
                if (keyboard.isLeftPressed() || keyboard.isAPressed()) {
                    face = Direction.LEFT
                    tryMove(Vec2(-1.0, 0.0))
                }
                else if (keyboard.isRightPressed() || keyboard.isDPressed()) {
                    face = Direction.RIGHT
                    tryMove(Vec2(1.0, 0.0))
                }
                else if (keyboard.isUpPressed() || keyboard.isWPressed()) {
                    face = Direction.UP
                    tryMove(Vec2(0.0, -1.0))
                }
                else if (keyboard.isDownPressed() || keyboard.isSPressed()) {
                    face = Direction.DOWN
                    tryMove(Vec2(0.0, 1.0))
                }
                else if (keyboard.isEscapePressed()) {
                    useContext<GameContext> {
                        isRunning = false
                    }
                }
            }
        }
    }

    private fun tryMove(delta: Vec2) {
        useContext<BoxxleContext> {
            val newP = p + delta

            // is a brick blocking the player?
            if (level.tiles[newP.y.toInt()][newP.x.toInt()] == Tiles.BRICK) return

            // is player pushing box
            for (box in level.boxes) {
                if (newP == box.p) {
                    if (canMoveBox(box.p + delta)) {
                        // move both the player and the box
                        isMoving = true
                        lastMovedMs = getCurrentTimestampMilliseconds()
                        return useContext<ActionsContext> {
                            moveTo(this@Player, newP, speed, onComplete = { isMoving = false })
                            moveTo(box, box.p + delta, speed, onComplete = { box.afterPush() })
                        }
                    }
                    return // player can't move if the box can't be pushed
                }
            }

            // if no box is being pushed, just move the player
            isMoving = true
            getContext<ActionsContext>().moveTo(this@Player, newP, speed, onComplete = { isMoving = false })
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

    override fun draw() {
        useContext<SpriteContext> {
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