import com.kengine.GameContext
import com.kengine.GameRunner
import com.kengine.context.useContext


fun main() {
    useContext(
        GameContext.create(
            title = "Kengine - Hello, World",
            width = 800,
            height = 600
        )
    ) {
        GameRunner(frameRate = 60) {
            HelloWorldGame()
        }
    }
}

