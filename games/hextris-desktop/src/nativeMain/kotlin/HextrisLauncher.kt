import com.kengine.PortableGameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import hextris.HextrisGame

fun main() {
    createGameContext(
        title = "Kengine - Hextris Desktop",
        width = 1280,
        height = 720,
        logLevel = Logger.Level.INFO
    ) {
        PortableGameRunner(
            frameRate = 60,
            commandCapacity = 1024
        ) {
            HextrisGame()
        }
    }
}
