import boxxle.BoxxleGame
import com.kengine.GameRunner
import com.kengine.createGameContext


fun main() {
    createGameContext(
        title = "Boxxle",
        width = 640,
        height = 480
    ) {
        GameRunner(frameRate = 60) {
            BoxxleGame()
        }
    }
}
