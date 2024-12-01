package com.kengine.map.tiled

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tileset(
    val firstgid: Int,
    val name: String,
    @SerialName("tilewidth")
    val tileWidth: Int,
    @SerialName("tileheight")
    val tileHeight: Int,
    @SerialName("imagewidth")
    val imageWidth: Int,
    @SerialName("imageheight")
    val imageHeight: Int,
    val image: String,
    val columns: Int,
    @SerialName("tilecount")
    val tileCount: Int,
    val margin: Int = 0,
    val spacing: Int = 0
)