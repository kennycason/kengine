import boxxle.BoxxleGame
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger

fun main() {
    createGameContext(
        title = "Boxxle",
        width = 640,
        height = 480,
        logLevel = Logger.Level.DEBUG
    ) {
        GameRunner(frameRate = 60) {
            BoxxleGame()
        }
    }
}
