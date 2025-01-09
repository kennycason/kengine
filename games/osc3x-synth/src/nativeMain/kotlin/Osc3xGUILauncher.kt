import com.kengine.GameRunner
import com.kengine.createGameContext
import osc3x.Osc3xGUI

fun main() {
    createGameContext(
        title = "Kengine - Osc3x Synth",
        width = 640,
        height = 480,
    ) {
        GameRunner(frameRate = 60) {
            Osc3xGUI()
        }
    }
}
