package games.boxxle

import com.kengine.Vec2
import com.kengine.context.useContext
import com.kengine.entity.Entity
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext

class Box(
    override val p: Vec2 = Vec2(),
    override val v: Vec2 = Vec2(),
    override val width: Int = 32,
    override val height: Int = 32,
    var scale: Double = 1.0
): Entity {
    private val spriteSheet = SpriteContext.get().manager.getSpriteSheet(Sprites.BOXXLE_SHEET)
    private val box = spriteSheet.getTile(1, 0)
    private val boxPlaced = spriteSheet.getTile(2, 0)
    private var isPlaced = false

    init {
        box.scale.set(scale)
        boxPlaced.scale.set(scale)
    }

    override fun update(elapsedSeconds: Double) {
    }

    override fun draw(elapsedSeconds: Double) {
        useContext(TextureContext.get()) {
            if (isPlaced) boxPlaced.draw(p.x * 32, p.y * 32)
            else box.draw(p.x * 32, p.y * 32)
        }
    }

    override fun cleanup() {
    }

    fun afterPush() {
        useContext(BoxxleContext.get()) {
            isPlaced = level.goals.any { goal -> goal.x == p.x && goal.y == p.y }
        }
    }
}