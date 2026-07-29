import com.kengine.PortableGameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import nintendoswitchdemo.NintendoSwitchDemoGame

fun main() {
    createGameContext(
        title = "Kengine - Nintendo Switch Demo",
        width = 1280,
        height = 720,
        logLevel = Logger.Level.INFO
    ) {
        PortableGameRunner(frameRate = 60) {
            NintendoSwitchDemoGame()
        }
    }
}
