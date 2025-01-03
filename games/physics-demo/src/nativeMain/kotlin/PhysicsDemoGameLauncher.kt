import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger

fun main() {
    createGameContext(
        title = "Kengine - Phsyics Demo",
        width = 800,
        height = 600,
        logLevel = Logger.Level.DEBUG
    ) {
        GameRunner(frameRate = 60) {
            PhysicsDemoGame()
        }
    }
}
