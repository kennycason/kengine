package boxxle

import boxxle.context.getBoxxleContext
import boxxle.context.useBoxxleContext
import com.kengine.entity.Entity
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.useTextureContext
import com.kengine.hooks.context.getContext
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
        val scaledDim = 32 * getBoxxleContext().level.data.scale
        useTextureContext {
            if (isPlaced) boxPlaced.draw(p.x * scaledDim, p.y * scaledDim)
            else box.draw(p.x * scaledDim, p.y * scaledDim)
        }
    }

    override fun cleanup() {
    }

    fun afterPush() {
        useBoxxleContext {
            isPlaced = level.goals.any { goal -> goal.x == p.x && goal.y == p.y }
        }
    }
}
