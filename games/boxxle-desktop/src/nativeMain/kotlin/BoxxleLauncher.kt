import boxxle.BoxxleAssets
import boxxle.BoxxleGame
import com.kengine.PortableGameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.sound.PortableAudioSdlRenderer
import com.kengine.sound.SoundContext

fun main() {
    createGameContext(
        title = "Kengine - Boxxle Desktop",
        width = 640,
        height = 480,
        logLevel = Logger.Level.INFO
    ) {
        registerContext(SoundContext.get())
        PortableGameRunner(
            frameRate = 60,
            commandCapacity = 512,
            audioSink = PortableAudioSdlRenderer(BoxxleAssets)
        ) {
            BoxxleGame()
        }
    }
}
