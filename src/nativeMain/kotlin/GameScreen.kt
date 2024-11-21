
import com.kengine.graphics.Sprite
import com.kengine.input.KeyboardController
import com.kengine.sdl.SDLContext
import sdl2.SDL_RenderClear
import sdl2.SDL_RenderPresent
import sdl2.SDL_SetRenderDrawColor
import kotlin.random.Random

class GameScreen {
    val sdlContext = SDLContext.get()
    private val keyboardController = KeyboardController()

    private val bulbasaurSprite = Sprite("images/bulbasaur.bmp")
    private val bulbasaur = Entity(
        x = (sdlContext.screenWidth / 2 - bulbasaurSprite.width / 2).toDouble(),
        y = (sdlContext.screenHeight / 2 - bulbasaurSprite.height / 2).toDouble(),
        vx = 0.0,
        vy = 0.0
    )

    private val pokeballSprite = Sprite("images/pokeball.bmp")
    private val pokeballs = List(size = 20) {
        Entity(
            x = Random.nextInt(0, sdlContext.screenWidth - pokeballSprite.width).toDouble(),
            y = Random.nextInt(0, sdlContext.screenHeight - pokeballSprite.height).toDouble(),
            vx = 100.0 * if (Random.nextBoolean()) 1 else -1,
            vy = 100.0 * if (Random.nextBoolean()) 1 else -1
        )
    }

    fun update(delta: Double) {
        pokeballs.forEach {
            it.x += it.vx * delta
            it.y += it.vy * delta
            if (it.x < 0 || it.x > sdlContext.screenWidth - pokeballSprite.width) {
                it.vx *= -1
            }
            if (it.y < 0 || it.y > sdlContext.screenHeight - pokeballSprite.height) {
                it.vy *= -1
            }
        }

        bulbasaur.vx *= 0.9
        bulbasaur.vy *= 0.9

        keyboardController.update()
        if (keyboardController.isLeftPressed()) {
            bulbasaur.vx = -10.0
        }
        if (keyboardController.isRightPressed()) {
            bulbasaur.vx = 10.0
        }
        if (keyboardController.isUpPressed()) {
            bulbasaur.vy = -10.0
        }
        if (keyboardController.isDownPressed()) {
            bulbasaur.vy = 10.0
        }

        bulbasaur.x += bulbasaur.vx
        bulbasaur.y += bulbasaur.vy
    }

    fun draw(delta: Double) {
        // clear screen
        SDL_SetRenderDrawColor(sdlContext.renderer, 0u, 0u, 0u, 255u)
        SDL_RenderClear(sdlContext.renderer)

        pokeballs.forEach {
            pokeballSprite.draw(it.x, it.y)
        }

        bulbasaurSprite.draw(bulbasaur.x, bulbasaur.y)

        // render to screen
        SDL_RenderPresent(sdlContext.renderer)
    }

    fun cleanup() {
        bulbasaurSprite.cleanup()
        pokeballSprite.cleanup()
    }

    data class Entity(
        var x: Double,
        var y: Double,
        var vx: Double,
        var vy: Double
    )

}
