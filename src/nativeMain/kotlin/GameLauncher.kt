
import com.kengine.GameLoop
import com.kengine.sdl.SDLContext

fun main() {
    val sdlContext = SDLContext.create(title = "Kengine Demo", width = 800, height = 600)
    val gameScreen = GameScreen()

    GameLoop(frameRate = 60) { delta ->
        gameScreen.update(delta)
        gameScreen.draw(delta)
    }

    gameScreen.cleanup()
    sdlContext.cleanup()
}

