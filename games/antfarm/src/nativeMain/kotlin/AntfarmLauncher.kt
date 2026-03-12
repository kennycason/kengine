import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.sound.SoundContext
import antfarm.AntfarmGame

fun main() {
    createGameContext(
        title = "Kengine - Antfarm Simulator",
        width = 1200,
        height = 800,
        logLevel = Logger.Level.DEBUG
    ) {
        registerContext(SoundContext.get())
        GameRunner(frameRate = 60) {
            AntfarmGame()
        }
    }
}

