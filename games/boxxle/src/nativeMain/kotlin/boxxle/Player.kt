package boxxle

import boxxle.context.getBoxxleContext
import boxxle.context.useBoxxleContext
import com.kengine.action.getActionContext
import com.kengine.action.useActionContext
import com.kengine.entity.Entity
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.useSpriteContext
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.controller.useControllerContext
import com.kengine.input.keyboard.useKeyboardContext
import com.kengine.log.Logging
import com.kengine.math.Vec2
import com.kengine.time.getCurrentMilliseconds
import com.kengine.time.timeSinceMs
import com.kengine.useGameContext

private enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

class Player(
    p: Vec2,
    private var scale: Double = 1.0
) : Entity(
    p = p, width = 32, height = 32,
), Logging {
    private val spriteSheet = SpriteContext.get().getSpriteSheet(Sprites.BOXXLE_SHEET)
    private val playerSpriteUp = spriteSheet.getTile(0, 1)
    private val playerSpriteDown = spriteSheet.getTile(1, 1)
    private val playerSpriteLeft = spriteSheet.getTile(2, 1)
    private val playerSpriteRight = spriteSheet.getTile(3, 1)
    private var face: Direction = Direction.DOWN
    private var lastMovedMs = 0L
    private var isMoving = false
    private val inputDelayMs = 100L
    private val moveDurationMs = 200L

    init {
        setScale(scale)
    }

    override fun update() {
        useKeyboardContext {
            if (!isMoving && timeSinceMs(lastMovedMs) > inputDelayMs) {
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

                if (keyboard.isEscapePressed()) {
                    useGameContext {
                        isRunning = false
                    }
                }
            }
        }
        useControllerContext {
            if (!isMoving && timeSinceMs(lastMovedMs) > inputDelayMs) {
                if (controller.isButtonPressed(Buttons.DPAD_LEFT)) {
                    face = Direction.LEFT
                    tryMove(Vec2(-1.0, 0.0))
                }
                if (controller.isButtonPressed(Buttons.DPAD_RIGHT)) {
                    face = Direction.RIGHT
                    tryMove(Vec2(1.0, 0.0))
                }
                if (controller.isButtonPressed(Buttons.DPAD_UP)) {
                    face = Direction.UP
                    tryMove(Vec2(0.0, -1.0))
                }
                else if (controller.isButtonPressed(Buttons.DPAD_DOWN)) {
                    face = Direction.DOWN
                    tryMove(Vec2(0.0, 1.0))
                }

                if (controller.isButtonPressed(Buttons.SELECT)) {
                    useGameContext {
                        isRunning = false
                    }
                }
            }
        }
    }

    private fun tryMove(delta: Vec2) {
        useBoxxleContext {
            val newP = p + delta

            // is a brick blocking the player?
            if (level.tiles[newP.y.toInt()][newP.x.toInt()] == Tiles.BRICK) return

            // is player pushing box
            for (box in level.boxes) {
                if (newP == box.p) {
                    if (canMoveBox(box.p + delta)) {
                        // move both the player and the box
                        isMoving = true
                        lastMovedMs = getCurrentMilliseconds()

                        return useActionContext {
                            moveTo(this@Player, newP, moveDurationMs, onComplete = {
                                isMoving = false
                                lastMovedMs = 0L
                            })
                            moveTo(box, box.p + delta, moveDurationMs, onComplete = { box.afterPush() })
                        }
                    }
                    return // player can't move if the box can't be pushed
                }
            }

            // if no box is being pushed, just move the player
            isMoving = true
            getActionContext().moveTo(this@Player, newP, moveDurationMs, onComplete = {
                isMoving = false
                lastMovedMs = 0L
            })
            lastMovedMs = getCurrentMilliseconds()
        }
    }

    private fun canMoveBox(newP: Vec2): Boolean {
        useBoxxleContext {
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
        useBoxxleContext {
            return p.x >= 0 && p.y >= 0 && p.x < level.tiles[0].size && p.y < level.tiles.size
        }
    }

    override fun draw() {
        val dim = getBoxxleContext().tileDim
        useSpriteContext {
            when (face) {
                Direction.UP -> playerSpriteUp.draw(p.x * dim, p.y * dim)
                Direction.DOWN -> playerSpriteDown.draw(p.x * dim, p.y * dim)
                Direction.LEFT -> playerSpriteLeft.draw(p.x * dim, p.y * dim)
                Direction.RIGHT -> playerSpriteRight.draw(p.x * dim, p.y * dim)
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
