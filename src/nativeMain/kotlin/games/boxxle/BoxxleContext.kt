package games.boxxle

import com.kengine.context.Context
import com.kengine.context.useContext
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.SpriteSheet

class BoxxleContext private constructor(
    var levelNumber: Int,
    var level: Level,
    val player: Player
) : Context() {

    companion object {
        private var currentContext: BoxxleContext? = null

        fun get(): BoxxleContext {
            if (currentContext == null) {
                loadSprites()

                val levelNumber = 5
                val level = Level(LEVEL_DATA[levelNumber])

                currentContext = BoxxleContext(
                    levelNumber = levelNumber,
                    level = level,
                    player = Player(p = level.start, scale = level.data.scale)
                )
            }
            return currentContext ?: throw IllegalStateException("Failed to create boxxle context")
        }

        private fun loadSprites() {
            useContext(SpriteContext.get()) {
                val spriteSheet = SpriteSheet(Sprite("images/boxxle/boxxle.bmp"), 32, 32)
                manager.setSpriteSheet("boxxle", spriteSheet)
            }
        }
    }

    override fun cleanup() {
    }

}