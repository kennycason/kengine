
import com.kengine.GameRunner
import com.kengine.createGameContext

fun main() {
    createGameContext(
        title = "Kengine - Osc3x Synth",
        width = 640,
        height = 480,
    ) {
        GameRunner(frameRate = 60) {
            Osc3xSynthAndVfx()
        }
    }
}
