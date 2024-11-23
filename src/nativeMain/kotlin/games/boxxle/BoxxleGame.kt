package games.boxxle

import com.kengine.Game
import com.kengine.context.useContext
import com.kengine.graphics.Sprite
import com.kengine.graphics.SpriteSheet
import com.kengine.sdl.SDLContext
import games.demo.entity.BulbasaurEntity
import sdl2.SDL_RenderClear
import sdl2.SDL_RenderPresent
import sdl2.SDL_SetRenderDrawColor

class BoxxleGame : Game {
    private val sprites = SpriteSheet(Sprite("images/boxxle/boxxle.bmp"), 32, 32)
    private val bulbasaur = BulbasaurEntity()

    override fun update(elapsedSeconds: Double) {
        bulbasaur.update(elapsedSeconds)
    }

    override fun draw(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            // clear screen
            SDL_SetRenderDrawColor(renderer, 255u, 255u, 255u, 255u)
            SDL_RenderClear(renderer)

            sprites.getTile(0, 0).draw(0.0, 0.0)
            sprites.getTile(0, 1).draw(0.0, 32.0)
            sprites.getTile(0, 2).draw(0.0, 64.0)
            sprites.getTile(1, 0).draw(32.0, 0.0)
            sprites.getTile(1, 1).draw(32.0, 32.0)
            sprites.getTile(1, 2).draw(32.0, 64.0)
            sprites.getTile(2, 0).draw(64.0, 0.0)
            sprites.getTile(2, 1).draw(64.0, 32.0)
            sprites.getTile(2, 2).draw(64.0, 64.0)

            bulbasaur.draw(elapsedSeconds)

            // render to screen
            SDL_RenderPresent(renderer)
        }
    }

    override fun cleanup() {
        bulbasaur.cleanup()
    }

}
