package games.boxxle

import com.kengine.Game
import com.kengine.context.useContext
import com.kengine.input.KeyboardContext
import com.kengine.sdl.SDLContext
import sdl2.SDLK_MINUS
import sdl2.SDLK_PLUS
import sdl2.SDLK_r

class BoxxleGame : Game {

    override fun update(elapsedSeconds: Double) {
        useContext(BoxxleContext.get()) {
            player.update(elapsedSeconds)

            useContext(KeyboardContext.get()) {
                if (keyboard.isKeyPressed(SDLK_r) && keyboard.timeSinceKeyPressed(SDLK_r) > 300u) {
                    loadLevel()
                }
                if (keyboard.isKeyPressed(SDLK_PLUS) && keyboard.timeSinceKeyPressed(SDLK_PLUS) > 300u) {
                    levelNumber = (levelNumber + 1) % LEVEL_DATA.size
                    loadLevel()
                }
                if (keyboard.isKeyPressed(SDLK_MINUS) && keyboard.timeSinceKeyPressed(SDLK_MINUS) > 300u) {
                    levelNumber = (levelNumber - 1 + LEVEL_DATA.size) % LEVEL_DATA.size
                    loadLevel()
                }
            }
        }
    }

    override fun draw(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            fillScreen(255u, 255u, 255u, 255u)

            useContext(BoxxleContext.get()) {
                level.draw(elapsedSeconds)
                player.draw(elapsedSeconds)
            }

            flipScreen()
        }
    }

    override fun cleanup() {
    }

    private fun loadLevel() {
        useContext(BoxxleContext.get()) {
            level = Level(LEVEL_DATA[levelNumber])
            player.p.set(level.start)
            player.setScale(level.data.scale)
        }
    }

}
