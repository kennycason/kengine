package games.demo

import com.kengine.Game
import com.kengine.context.useContext
import com.kengine.sdl.SDLContext
import games.demo.entity.BouncingPokeballEntity
import games.demo.entity.BulbasaurEntity
import sdl2.SDL_RenderClear
import sdl2.SDL_RenderPresent
import sdl2.SDL_SetRenderDrawColor

class DemoGame : Game {
    private val bulbasaur = BulbasaurEntity()
    private val pokeballs = List(size = 50) { BouncingPokeballEntity() }

    override fun update(elapsedSeconds: Double) {
        pokeballs.forEach {
            it.update(elapsedSeconds)
        }
        bulbasaur.update(elapsedSeconds)
    }

    override fun draw(elapsedSeconds: Double) {
        useContext(SDLContext.get()) {
            // clear screen
            SDL_SetRenderDrawColor(renderer, 0u, 0u, 0u, 255u)
            SDL_RenderClear(renderer)

            pokeballs.forEach {
                it.draw(elapsedSeconds)
            }
            bulbasaur.draw(elapsedSeconds)

            // render to screen
            SDL_RenderPresent(renderer)
        }
    }

    override fun cleanup() {
        bulbasaur.cleanup()
        pokeballs.forEach { it.cleanup() }
    }

}
