
import kengine.playdate.KenginePlaydateGame
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import playdate.api.PlaydateAPI
import kotlin.experimental.ExperimentalNativeApi

private var game: KenginePlaydateGame? = null

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("_startKengineGame")
fun startKengineGame(playdate: CPointer<PlaydateAPI>): Int {
    game = KenginePlaydateGame(playdate)
    return 0
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("_updateKengineGame")
fun updateKengineGame(): Int {
    game?.let {
        it.update()
        it.draw()
    }
    return 1
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("_cleanupKengineGame")
fun cleanupKengineGame() {
    game?.cleanup()
    game = null
}