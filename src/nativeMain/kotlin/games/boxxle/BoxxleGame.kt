package games.boxxle

import com.kengine.Game
import com.kengine.context.useContext
import com.kengine.input.KeyboardContext
import com.kengine.sdl.SDLContext
import com.kengine.time.getCurrentTimestampMilliseconds
import sdl2.SDLK_RETURN
import sdl2.SDLK_SPACE
import sdl2.SDLK_r

class BoxxleGame : Game {

    private var timeSinceOptionChange = 0L // TODO fix keyboard.timeSinceKeyPressed function

    override fun update(elapsedSeconds: Double) {
        useContext(BoxxleContext.get()) {
            player.update(elapsedSeconds)

            useContext(KeyboardContext.get()) {
                if (keyboard.isKeyPressed(SDLK_r) && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    loadLevel()
                }
                if (keyboard.isKeyPressed(SDLK_RETURN) && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    levelNumber = (levelNumber + 1) % LEVEL_DATA.size
                    loadLevel()
                }
                if (keyboard.isKeyPressed(SDLK_SPACE) && getCurrentTimestampMilliseconds() - timeSinceOptionChange > 300) {
                    timeSinceOptionChange = getCurrentTimestampMilliseconds()
                    levelNumber = (levelNumber - 1 + LEVEL_DATA.size) % LEVEL_DATA.size
                    loadLevel()
                }
            }

            if (isLevelComplete()) {
                levelNumber = (levelNumber + 1 + LEVEL_DATA.size) % LEVEL_DATA.size
                loadLevel()
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

    private fun isLevelComplete(): Boolean {
        useContext(BoxxleContext.get()) {
            for (goal in level.goals) {
                if (!level.boxes.any { it.p == goal }) {
                    return false // a goal doesn't have a box on it
                }
            }
            return true // all goals must have boxes
        }
    }

}
