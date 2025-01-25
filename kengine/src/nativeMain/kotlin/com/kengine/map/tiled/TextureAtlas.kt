package com.kengine.map.tiled

import com.kengine.graphics.Texture
import com.kengine.math.IntRect

class TextureAtlas(
    val texture: Texture,
    private val regions: Map<String, IntRect>
) {
    fun getRegion(name: String): IntRect = regions.getValue(name)
}
