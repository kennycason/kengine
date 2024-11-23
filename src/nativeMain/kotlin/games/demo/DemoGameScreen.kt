package games.demo

import com.kengine.sdl.SDLContext
import games.demo.entity.BouncingPokeballEntity
import games.demo.entity.BulbasaurEntity
import sdl2.SDL_RenderClear
import sdl2.SDL_RenderPresent
import sdl2.SDL_SetRenderDrawColor

class DemoGameScreen {
    private val bulbasaur = BulbasaurEntity()
    private val pokeballs = List(size = 50) { BouncingPokeballEntity() }

    fun update(elapsedSeconds: Double) {
        pokeballs.forEach {
            it.update(elapsedSeconds)
        }
        bulbasaur.update(elapsedSeconds)
    }

    fun draw(elapsedSeconds: Double) {
        val sdlContext = SDLContext.get()

        // clear screen
        SDL_SetRenderDrawColor(sdlContext.renderer, 0u, 0u, 0u, 255u)
        SDL_RenderClear(sdlContext.renderer)

        pokeballs.forEach {
            it.draw(elapsedSeconds)
        }
        bulbasaur.draw(elapsedSeconds)

        // render to screen
        SDL_RenderPresent(sdlContext.renderer)
    }

    fun cleanup() {
        bulbasaur.cleanup()
        pokeballs.forEach { it.cleanup() }
    }

}
