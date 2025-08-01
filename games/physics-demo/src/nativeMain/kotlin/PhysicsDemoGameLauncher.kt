import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.physics.PhysicsContext

fun main() {
    createGameContext(
        title = "Kengine - Physics Demo",
        width = 800,
        height = 600,
        logLevel = Logger.Level.DEBUG
    ) {
        registerContext(PhysicsContext.get())
        GameRunner(frameRate = 60) {
            PhysicsDemoGame()
        }
    }
}
