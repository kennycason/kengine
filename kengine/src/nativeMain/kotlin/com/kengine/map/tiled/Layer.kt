package com.kengine.map.tiled

import kotlinx.serialization.Serializable

@Serializable
data class Layer(
    val id: Int,
    val name: String,
    val type: String,
    val visible: Boolean,
    val width: Int,
    val height: Int,
    val data: String,
    val encoding: String,
    val opacity: Double,
    val x: Int,
    val y: Int
) {
    val decodedData = LayerDataDecoder.decode(data, encoding)

    fun getTileAt(x: Int, y: Int): Int {
        return decodedData[y * width + x]
    }

    override fun toString(): String {
        return "Layer(id=$id, name='$name', type='$type', visible=$visible, width=$width, height=$height, data='$data', encoding='$encoding', opacity=$opacity, x=$x, y=$y, decodedData=$decodedData)"
    }

}