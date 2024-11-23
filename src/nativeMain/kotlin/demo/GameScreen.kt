package demo

import com.kengine.sdl.SDLContext
import demo.entity.BouncingPokeballEntity
import demo.entity.BulbasaurEntity
import sdl2.SDL_RenderClear
import sdl2.SDL_RenderPresent
import sdl2.SDL_SetRenderDrawColor

class GameScreen {
    private val bulbasaur = BulbasaurEntity()
    private val pokeballs = List(size = 50) { BouncingPokeballEntity() }

    fun update(elapsedSeconds: Double) {
        pokeballs.forEach {
            it.update(elapsedSeconds)
        }
        bulbasaur.update(elapsedSeconds)
    }

    fun draw(elapsedSeconds: Double) {
        val sdlKontext = SDLContext.get()

        // clear screen
        SDL_SetRenderDrawColor(sdlKontext.renderer, 0u, 0u, 0u, 255u)
        SDL_RenderClear(sdlKontext.renderer)

        pokeballs.forEach {
            it.draw(elapsedSeconds)
        }
        bulbasaur.draw(elapsedSeconds)

        // render to screen
        SDL_RenderPresent(sdlKontext.renderer)
    }

    fun cleanup() {
        bulbasaur.cleanup()
        pokeballs.forEach { it.cleanup() }
    }

}
