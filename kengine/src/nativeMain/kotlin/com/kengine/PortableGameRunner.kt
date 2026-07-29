package com.kengine

import com.kengine.render.PortableSpriteRegistry

class PortableGameRunner(
    frameRate: Int = 60,
    spriteRegistry: PortableSpriteRegistry = PortableSpriteRegistry(),
    gameBuilder: () -> PortableGame
) {
    init {
        GameRunner(frameRate) {
            PortableGameAdapter(
                portableGame = gameBuilder(),
                spriteRegistry = spriteRegistry
            )
        }
    }
}
