import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger

fun main() {
    createGameContext(
        title = "Kengine - Hello, World",
        width = 800,
        height = 600,
        logLevel = Logger.Level.INFO
    ) {
        GameRunner(frameRate = 60) { // max fps
            HelloWorldGame()
        }
    }
}
