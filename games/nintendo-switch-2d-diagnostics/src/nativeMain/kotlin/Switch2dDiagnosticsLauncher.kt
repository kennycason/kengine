import com.kengine.PortableGameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.render.PortableSpriteRegistry
import switchdiagnostics.DIAGNOSTICS_SPRITES
import switchdiagnostics.Switch2dDiagnosticsGame

fun main() {
    createGameContext(
        title = "Kengine - Nintendo Switch 2D Diagnostics",
        width = 1280,
        height = 720,
        logLevel = Logger.Level.INFO
    ) {
        val spriteRegistry = PortableSpriteRegistry()
            .registerSpriteSheetFromFilePath(DIAGNOSTICS_SPRITES, "assets/sprites/diagnostics_sprites.png", 32, 32)

        PortableGameRunner(
            frameRate = 60,
            spriteRegistry = spriteRegistry
        ) {
            Switch2dDiagnosticsGame()
        }
    }
}
