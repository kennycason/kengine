package games.boxxle

import com.kengine.Game
import com.kengine.context.useContext
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteContext
import com.kengine.graphics.SpriteSheet
import com.kengine.input.KeyboardContext
import com.kengine.sdl.SDLContext

class BoxxleGame : Game {
    private var levelNumber = 0
    private lateinit var level: Level
    private lateinit var player: Player

    init {
        useContext(SpriteContext.get()) {
            val spriteSheet = SpriteSheet(Sprite("images/boxxle/boxxle.bmp"), 32, 32)
            manager.setSpriteSheet("boxxle", spriteSheet)

            level = Level(LEVEL_DATA[levelNumber])
            player = Player(scale = level.data.scale)
        }
    }

    override fun update(elapsedSeconds: Double) {
        player.update(elapsedSeconds)

        useContext(KeyboardContext.get()) {
            if (keyboard.isAPressed()) {
                levelNumber = (levelNumber + 1) % LEVEL_DATA.size
                loadLevel()
            }
            if (keyboard.isBPressed()) {
                levelNumber = (levelNumber - 1 + LEVEL_DATA.size) % LEVEL_DATA.size
                loadLevel()
            }
        }
    }

    override fun draw(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            fillScreen(255u, 255u, 255u, 255u)

            level.draw(elapsedSeconds)
            player.draw(elapsedSeconds)

            flipScreen()
        }
    }

    override fun cleanup() {
    }

    private fun loadLevel() {
        level = Level(LEVEL_DATA[levelNumber])
        player.scale = level.data.scale
    }

}
