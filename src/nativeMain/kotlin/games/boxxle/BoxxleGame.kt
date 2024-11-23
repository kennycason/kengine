package games.boxxle

import com.kengine.Game
import com.kengine.context.useContext
import com.kengine.input.KeyboardContext
import com.kengine.log.Logger
import com.kengine.sdl.SDLContext
import sdl2.SDL_RenderClear
import sdl2.SDL_RenderPresent
import sdl2.SDL_SetRenderDrawColor

class BoxxleGame : Game {
    private var levelNumber = 0
    private var level = Level(LEVEL_DATA[levelNumber])

    override fun update(elapsedSeconds: Double) {
        useContext(KeyboardContext.get()) {
            if (keyboard.isUpPressed()) {
                levelNumber = (levelNumber + 1) % LEVEL_DATA.size
                Logger.info(levelNumber)
                level = Level(LEVEL_DATA[levelNumber])
            }
            if (keyboard.isDownPressed()) {
                levelNumber = (levelNumber - 1 + LEVEL_DATA.size) % LEVEL_DATA.size
                Logger.info(levelNumber)
                level = Level(LEVEL_DATA[levelNumber])
            }
        }

    }

    override fun draw(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            // clear screen
            SDL_SetRenderDrawColor(renderer, 255u, 255u, 255u, 255u)
            SDL_RenderClear(renderer)

            level.draw()

            // render to screen
            SDL_RenderPresent(renderer)
        }
    }

    override fun cleanup() {
    }

}
