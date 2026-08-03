import com.kengine.PortableGameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.sound.PortableAudioSdlRenderer
import com.kengine.sound.SoundContext
import hextris.HextrisAssets
import hextris.HextrisGame

fun main() {
    createGameContext(
        title = "Kengine - Hextris Desktop",
        width = 1280,
        height = 720,
        logLevel = Logger.Level.INFO
    ) {
        registerContext(SoundContext.get())
        PortableGameRunner(
            frameRate = 60,
            commandCapacity = 1024,
            audioSink = PortableAudioSdlRenderer(HextrisAssets)
        ) {
            HextrisGame()
        }
    }
}
