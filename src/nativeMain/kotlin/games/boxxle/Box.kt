package games.boxxle

import com.kengine.Vec2
import com.kengine.context.useContext
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext

class Box(
    private val p: Vec2 = Vec2(),
    scale: Double = 1.0
) {
    private val spriteSheet = SpriteContext.get().manager.getSpriteSheet("boxxle")
    private val box = spriteSheet.getTile(1, 0)
    private val boxPlaced = spriteSheet.getTile(2, 0)
    private var isMoving = false
    private var isPlaced = false

    init {
        box.scale.set(scale)
        boxPlaced.scale.set(scale)
    }

    fun move(newP: Vec2, goals: List<Vec2>) {
        if (isMoving) return

        isMoving = true
        p.set(newP)

        isMoving = false
        afterPush(goals)
    }

    private fun afterPush(goals: List<Vec2>) {
        isPlaced = goals.any { goal -> goal.x == p.x && goal.y == p.y }
    }

    fun draw() {
        useContext(TextureContext.get()) {
            if (isPlaced) boxPlaced.draw(p.x * 32, p.y * 32)
            else box.draw(p.x * 32, p.y * 32)
        }
    }
}