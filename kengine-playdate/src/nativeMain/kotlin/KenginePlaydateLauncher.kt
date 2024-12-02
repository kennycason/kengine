import com.kengine.GameRunner
import com.kengine.createGameContext
import kengine.playdate.KenginePlaydateGame
import playdate.api.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("startKengineGame")
fun startKengineGame(playdate: CPointer<PlaydateAPI>): Int {
    createGameContext(
        title = "Kengine Playdate Launcher",
        width = 400,
        height = 240
    ) {
        GameRunner(frameRate = 30) {
            KenginePlaydateGame()
        }
    }
    return 1
}