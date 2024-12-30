
import com.kengine.entity.Entity
import com.kengine.graphics.AnimatedSprite
import com.kengine.graphics.SpriteSheet
import com.kengine.time.getClockContext

class ScytherEntity : Entity(width = 56, height = 56) {
    private val spriteSheet = SpriteSheet.fromFilePath("assets/sprites/scyther.bmp", tileWidth = 56, tileHeight = 56)
    private val animatedScyther = AnimatedSprite.fromSpriteSheet(spriteSheet, frameDurationMs = 200L)
        .also { p.set(200.0, 200.0) }

    override fun update() {
        animatedScyther.update()
    }

    override fun draw() {
        animatedScyther.draw(p.x, p.y)
    }

    override fun cleanup() {
    }

}
