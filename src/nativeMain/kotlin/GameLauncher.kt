
import com.kengine.GameLoop
import com.kengine.context.AppContext
import com.kengine.context.useContext
import com.kengine.log.Logger
import games.demo.DemoGameScreen


fun main() {
    try {
        AppContext.create(title = "Kengine Demo", width = 800, height = 600)
        useContext(AppContext.get(), cleanup = true) {
            val gameScreen = DemoGameScreen()

            GameLoop(frameRate = 60) { elapsedSeconds ->
                gameScreen.update(elapsedSeconds)
                gameScreen.draw(elapsedSeconds)
            }

            gameScreen.cleanup()
        }

    } catch (e: Exception) {
        Logger.error { "Unhandled exception in GameLoop: ${e.message}" }
        Logger.error { "Stacktrace: ${e.stackTraceToString()}" }
        e.printStackTrace()
    }

}

