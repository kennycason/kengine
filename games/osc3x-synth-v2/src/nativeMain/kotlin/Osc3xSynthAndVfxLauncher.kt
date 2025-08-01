
import com.kengine.GameContext
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.sound.SoundContext

fun main() {
    createGameContext(
        title = "Kengine - Osc3x Synth",
        width = 640,
        height = 480,
    ) {
        registerContext(SoundContext.get())
        GameRunner(frameRate = 60) {
            Osc3xSynthAndVfx()
        }
    }
}
