package boxxle

import com.kengine.context.Context
import com.kengine.context.useContext
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.SpriteSheet

class BoxxleContext private constructor(
    var level: Level,
    val player: Player
) : Context() {

    companion object {
        private var currentContext: BoxxleContext? = null

        fun get(): BoxxleContext {
            if (currentContext == null) {
                loadSprites()

                val level = Level(0)

                currentContext = BoxxleContext(
                    level = level,
                    player = Player(p = level.start, scale = level.data.scale)
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create boxxle context")
        }

        private fun loadSprites() {
            useContext(SpriteContext.get()) {
                val spriteSheet = SpriteSheet(Sprite(Sprites.BOXXLE_SHEET_BMP), 32, 32)
                manager.setSpriteSheet(Sprites.BOXXLE_SHEET, spriteSheet)
            }
        }
    }

    override fun cleanup() {
    }

}