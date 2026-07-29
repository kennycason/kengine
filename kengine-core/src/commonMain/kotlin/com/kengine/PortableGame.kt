package com.kengine

import com.kengine.assets.EmptyPortableAssetCatalog
import com.kengine.assets.PortableAssetCatalog
import com.kengine.audio.AudioContext
import com.kengine.input.InputState
import com.kengine.render.RenderContext

interface PortableGame {
    val assets: PortableAssetCatalog
        get() = EmptyPortableAssetCatalog

    fun update(input: InputState)
    fun audio(audio: AudioContext) {
    }
    fun draw(render: RenderContext)
    fun cleanup()
}
