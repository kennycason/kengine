import com.kengine.GameLoop
import com.kengine.context.SDLKontext

fun main() {
    SDLKontext.create(title = "Kengine Demo", width = 800, height = 600)

    val gameScreen = GameScreen()
    GameLoop(frameRate = 60) { delta ->
        gameScreen.update(delta)
        gameScreen.draw(delta)
    }

    gameScreen.cleanup()
    SDLKontext.get().cleanup()
}

