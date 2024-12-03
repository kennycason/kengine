import com.kengine.GameRunner
import com.kengine.createGameContext


fun main() {
    createGameContext(
        title = "Boxxle",
        width = 400,
        height = 240,
    ) {
        GameRunner(frameRate = 60) {
            HelloWorldGame()
        }
    }
}

