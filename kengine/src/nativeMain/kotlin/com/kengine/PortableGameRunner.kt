package com.kengine

import com.kengine.audio.PortableAudioSink
import com.kengine.render.PortableSpriteRegistry
import com.kengine.storage.PortableFileStorage
import com.kengine.storage.PortableStorage

class PortableGameRunner(
    frameRate: Int = 60,
    spriteRegistry: PortableSpriteRegistry = PortableSpriteRegistry(),
    commandCapacity: Int = 256,
    audioSink: PortableAudioSink = PortableAudioSink.NoOp,
    storageBuilder: (PortableGame) -> PortableStorage = { game -> PortableFileStorage(game.storageNamespace) },
    gameBuilder: () -> PortableGame
) {
    init {
        GameRunner(frameRate) {
            val portableGame = gameBuilder()
            spriteRegistry.registerAssetsFromFilePaths(portableGame.assets)
            portableGame.attachStorage(storageBuilder(portableGame))
            PortableGameAdapter(
                portableGame = portableGame,
                spriteRegistry = spriteRegistry,
                commandCapacity = commandCapacity,
                audioSink = audioSink
            )
        }
    }
}
