
import com.kengine.entity.Entity
import com.kengine.graphics.AnimatedSprite
import com.kengine.graphics.SpriteSheet

class ScytherEntity : Entity(width = 56, height = 56) {
    private val spriteSheet = SpriteSheet.fromFilePath("assets/sprites/scyther.bmp", tileWidth = 56, tileHeight = 56)
    private val animatedScyther = AnimatedSprite.fromSpriteSheet(spriteSheet, frameDurationMs = 200L)
        .also { p.set(200.0, 200.0) }

    override fun update() {
    }

    override fun draw() {
        animatedScyther.draw(p)
    }

    override fun cleanup() {
    }

}