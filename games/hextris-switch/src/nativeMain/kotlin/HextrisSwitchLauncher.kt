import com.kengine.PortableGameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import com.kengine.render.PortableSpriteRegistry
import hextrisswitch.HextrisSwitchGame
import hextrisswitch.Sprites

fun main() {
    createGameContext(
        title = "Kengine - Hextris Switch",
        width = 1280,
        height = 720,
        logLevel = Logger.Level.INFO
    ) {
        val spriteRegistry = PortableSpriteRegistry()
            .registerSpriteSheetFromFilePath(
                name = Sprites.BLOCK_SPRITE_ID,
                filePath = Sprites.BLOCK_SPRITES,
                tileWidth = Sprites.BLOCK_SIZE,
                tileHeight = Sprites.BLOCK_SIZE
            )

        PortableGameRunner(
            frameRate = 60,
            spriteRegistry = spriteRegistry,
            commandCapacity = 1024
        ) {
            HextrisSwitchGame()
        }
    }
}
