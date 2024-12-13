package com.kengine.map.tiled

import kotlinx.serialization.Serializable

@Serializable
data class Layer(
    val id: Int,
    val name: String,
    val type: String,
    val visible: Boolean = true,
    val opacity: Float = 1f,
    val x: Int = 0,
    val y: Int = 0,
    // optional since object layers don't have them
    val width: Int? = null,
    val height: Int? = null,
    val data: String? = null,
    val encoding: String? = null,
    val compression: String? = null,
    // object layer fields
    val draworder: String? = null,
    val objects: List<TiledObject>? = null
) {
    val decodedData = LayerDataDecoder.decode(this)

    fun getTileAt(x: Int, y: Int): Int {
        if (type != "tilelayer" || data == null || encoding == null || width == null || height == null) {
            return throw IllegalArgumentException("Layer [$name] is not a tile layer")
        }

        return decodedData[y * width + x]
    }
    override fun toString(): String {
        return "Layer(id=$id, name='$name', type='$type', visible=$visible, width=$width, height=$height, data='$data', encoding='$encoding', opacity=$opacity, x=$x, y=$y, decodedData=$decodedData)"
    }
}


