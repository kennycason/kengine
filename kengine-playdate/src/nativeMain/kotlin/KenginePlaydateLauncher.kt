
import kengine.playdate.KenginePlaydateGame
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import playdate.api.PlaydateAPI
import kotlin.experimental.ExperimentalNativeApi


@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("_startKengineGame")
fun startKengineGame(playdate: CPointer<PlaydateAPI>): Int {
    val game = KenginePlaydateGame()
    while (true) {
        game.update()
        game.draw()
    }
    return 1
}