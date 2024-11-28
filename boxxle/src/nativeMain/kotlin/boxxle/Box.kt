package boxxle

import com.kengine.context.getContext
import com.kengine.context.useContext
import com.kengine.entity.Entity
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.TextureContext
import com.kengine.math.Vec2

class Box(
    p: Vec2,
    var scale: Double = 1.0
): Entity(
    p = p, width = 32, height = 32
) {
    private val spriteSheet = getContext<SpriteContext>().getSpriteSheet(Sprites.BOXXLE_SHEET)
    private val box = spriteSheet.getTile(1, 0)
    private val boxPlaced = spriteSheet.getTile(2, 0)
    private var isPlaced = false

    init {
        box.scale.set(scale)
        boxPlaced.scale.set(scale)
    }

    override fun update() {
    }

    override fun draw() {
        useContext<TextureContext> {
            if (isPlaced) boxPlaced.draw(p.x * 32, p.y * 32)
            else box.draw(p.x * 32, p.y * 32)
        }
    }

    override fun cleanup() {
    }

    fun afterPush() {
        useContext<BoxxleContext> {
            isPlaced = level.goals.any { goal -> goal.x == p.x && goal.y == p.y }
        }
    }
}