import com.kengine.GameRunner
import com.kengine.createGameContext


fun main() {
    createGameContext(
        title = "Kengine - Phsyics Demo",
        width = 800,
        height = 600,
    ) {
        GameRunner(frameRate = 60) {
            PhysicsDemoGame()
        }
    }
}

