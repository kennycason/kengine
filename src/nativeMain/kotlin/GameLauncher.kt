import com.kengine.GameContext
import com.kengine.GameLoop
import com.kengine.context.useContext
import com.kengine.log.Logger
import games.boxxle.BoxxleGame


fun main() {
    try {
        GameContext.create(title = "Boxxle", width = 800, height = 600)
        useContext(GameContext.get(), cleanup = true) {
            val boxxle = BoxxleGame()

            GameLoop(frameRate = 60) { elapsedSeconds ->
                boxxle.update(elapsedSeconds)
                boxxle.draw(elapsedSeconds)
            }

            boxxle.cleanup()
        }
    } catch (e: Exception) {
        Logger.error { "Unhandled exception in GameLoop: ${e.message}" }
        Logger.error { "Stacktrace: ${e.stackTraceToString()}" }
        e.printStackTrace()
    }

}

