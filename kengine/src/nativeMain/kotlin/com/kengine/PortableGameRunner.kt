package com.kengine

import com.kengine.render.PortableSpriteRegistry

class PortableGameRunner(
    frameRate: Int = 60,
    spriteRegistry: PortableSpriteRegistry = PortableSpriteRegistry(),
    commandCapacity: Int = 256,
    gameBuilder: () -> PortableGame
) {
    init {
        GameRunner(frameRate) {
            val portableGame = gameBuilder()
            spriteRegistry.registerAssetsFromFilePaths(portableGame.assets)
            PortableGameAdapter(
                portableGame = portableGame,
                spriteRegistry = spriteRegistry,
                commandCapacity = commandCapacity
            )
        }
    }
}
