import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.sound.SoundContext
import hextris.HextrisGame

fun main() {
    createGameContext(
        title = "Kengine - Hextris",
        width = 800,
        height = 610,
        logLevel = Logger.Level.DEBUG
    ) {
        registerContext(SoundContext.get())
        GameRunner(frameRate = 60) {
            HextrisGame()
        }
    }
}
