package com.kengine

import com.kengine.assets.EmptyPortableAssetCatalog
import com.kengine.assets.PortableAssetCatalog
import com.kengine.audio.AudioContext
import com.kengine.input.InputState
import com.kengine.render.RenderContext
import com.kengine.storage.PortableStorage

interface PortableGame {
    val assets: PortableAssetCatalog
        get() = EmptyPortableAssetCatalog

    val storageNamespace: String
        get() = "kengine"

    fun attachStorage(storage: PortableStorage) {
    }

    fun update(input: InputState)
    fun audio(audio: AudioContext) {
    }
    fun draw(render: RenderContext)
    fun cleanup()
}
