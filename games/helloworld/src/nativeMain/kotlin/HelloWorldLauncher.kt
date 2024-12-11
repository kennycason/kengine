import com.kengine.GameRunner
import com.kengine.createGameContext


fun main() {
    createGameContext(
        title = "Kengine - Hello, World",
        width = 800,
        height = 600,
    ) {
        GameRunner(frameRate = 60) {
            HelloWorldGame()
        }
    }
}

