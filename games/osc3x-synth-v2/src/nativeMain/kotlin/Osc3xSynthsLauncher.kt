
import com.kengine.GameRunner
import com.kengine.createGameContext
import osc3x.v2.Osc3xSynths

fun main() {
    createGameContext(
        title = "Kengine - Osc3x Synths",
        width = 640,
        height = 480,
    ) {
        GameRunner(frameRate = 60) {
            Osc3xSynths()
        }
    }
}
