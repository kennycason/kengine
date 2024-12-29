package boxxle.context

import boxxle.Level
import boxxle.Player
import boxxle.Sprites
import com.kengine.graphics.SpriteSheet
import com.kengine.graphics.useSpriteContext
import com.kengine.hooks.context.Context

class BoxxleContext private constructor(
    var level: Level,
    val player: Player
) : Context() {

    val tileDim = 32

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
            useSpriteContext {
                addSpriteSheet(
                    Sprites.BOXXLE_SHEET,
                    SpriteSheet.fromFilePath(Sprites.BOXXLE_SHEET_BMP, 32, 32)
                )
            }
        }
    }

    override fun cleanup() {
    }

}
