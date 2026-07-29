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
            PortableGameAdapter(
                portableGame = gameBuilder(),
                spriteRegistry = spriteRegistry,
                commandCapacity = commandCapacity
            )
        }
    }
}
