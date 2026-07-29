package com.kengine

class PortableGameRunner(
    frameRate: Int = 60,
    gameBuilder: () -> PortableGame
) {
    init {
        GameRunner(frameRate) {
            PortableGameAdapter(gameBuilder())
        }
    }
}
