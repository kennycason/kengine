import com.kengine.PortableGameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.render.PortableSpriteRegistry
import nintendoswitchdemo.DEMO_BLOCK_SPRITES
import nintendoswitchdemo.DEMO_POKEBALL_SPRITE
import nintendoswitchdemo.NintendoSwitchDemoGame

fun main() {
    createGameContext(
        title = "Kengine - Nintendo Switch Demo",
        width = 1280,
        height = 720,
        logLevel = Logger.Level.INFO
    ) {
        val spriteRegistry = PortableSpriteRegistry()
            .registerSpriteFromFilePath(DEMO_POKEBALL_SPRITE, "assets/sprites/pokeball.bmp")
            .registerSpriteSheetFromFilePath(DEMO_BLOCK_SPRITES, "assets/sprites/block_sprites.png", 24, 24)

        PortableGameRunner(
            frameRate = 60,
            spriteRegistry = spriteRegistry
        ) {
            NintendoSwitchDemoGame()
        }
    }
}
