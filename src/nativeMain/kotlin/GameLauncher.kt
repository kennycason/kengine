import com.kengine.GameContext
import com.kengine.GameRunner
import com.kengine.context.useContext
import games.boxxle.BoxxleGame


fun main() {
    useContext(
        GameContext.create(
            title = "Boxxle",
            width = 800,
            height = 600
        )
    ) {
        GameRunner(frameRate = 60) {
            BoxxleGame()
        }
    }
}

