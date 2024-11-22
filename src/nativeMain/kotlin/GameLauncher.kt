
import com.kengine.GameLoop
import com.kengine.context.AppContext
import com.kengine.context.useContext
import demo.GameScreen


fun main() {
    try {
        AppContext.create(title = "Kengine Demo", width = 800, height = 600)
        useContext(AppContext.get(), cleanup = true) {
            val gameScreen = GameScreen()

            GameLoop(frameRate = 60) { elapsedSeconds ->
                gameScreen.update(elapsedSeconds)
                gameScreen.draw(elapsedSeconds)
            }

            gameScreen.cleanup()
        }

    } catch (e: Exception) {
        println("Unhandled exception in GameLoop: ${e.message}")
        e.printStackTrace()
    }

}

