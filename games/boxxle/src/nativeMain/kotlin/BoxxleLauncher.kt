import boxxle.BoxxleGame
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.sound.SoundContext

fun main() {
    createGameContext(
        title = "Kengine - Boxxle",
        width = 640,
        height = 480,
        logLevel = Logger.Level.DEBUG
    ) {
        registerContext(SoundContext.get())
        GameRunner(frameRate = 60) {
            BoxxleGame()
        }
    }
}
