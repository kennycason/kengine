import boxxle.BoxxleGame
import com.kengine.GameRunner
import com.kengine.createGameContext


fun main() {
    createGameContext(
        title = "Boxxle",
        width = 800,
        height = 600
    ) {
        GameRunner(frameRate = 60) {
            BoxxleGame()
        }
    }
}

